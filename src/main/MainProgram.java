package main;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;

import pa.cl.CLUtil;
import pa.cl.OpenCL;
import pa.cl.CLUtil.PlatformDevicePair;
import pa.util.IOUtil;
import pa.util.SizeOf;

import opengl.GL;
import static opengl.GL.*;
import opengl.util.Camera;
import opengl.util.ShaderProgram;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;

import particle.Particle;
import particle.ParticleFactory;
import opengl.util.FrameBuffer;
import opengl.util.Texture;
import opengl.util.Geometry;
import opengl.util.GeometryFactory;

public class MainProgram {
	private boolean running = true;
	
	////// SHARED BLOCK
	private int bufferObjectPositions = -1;
	private int bufferObjectLifetimes = -1;
	private int elements = 1<<8; // 2^n = 1<<n 

	////// OPENCL BLOCK
	private CLContext context;
	private CLCommandQueue queue;
	private CLProgram program;
	private CLKernel kernelMove;
	private CLMem memPositions;
	private CLMem memLifetime;

	////// OPENGL BLOCK
	private ShaderProgram shaderProgram  = null;
	private Matrix4f modelMat = new Matrix4f();
//	private Matrix4f modelIT  = opengl.util.Util.transposeInverse(modelMat, null);
	private Camera   cam      = new Camera();
	private int vertexArrayID = -1;
	
	////// other
	private long lastTimestamp = System.currentTimeMillis();
	private int numberOfFrames = 0;
	
	////// deferred shading
	private Geometry screenQuad;
	private int textureUnit = 0;
	private ShaderProgram depthSP;
    private FrameBuffer depthFB;
    private Texture depthTex;
    private Texture depth2Tex;
    private Texture depth3Tex;
    private ShaderProgram drawTextureSP;
	
	
	
	
	
	public MainProgram() {
		initGL();
		initCL();

		

		screenQuad = GeometryFactory.createScreenQuad();
//		depthSP = new ShaderProgram("./shader/Depth_VS.glsl", "./shader/Depth_FS.glsl");
		drawTextureSP = new ShaderProgram("shader/ScreenQuad_VS.glsl", "shader/CopyTexture_FS.glsl");
		depthSP = new ShaderProgram("./shader/DefaultVS.glsl", "./shader/Default1FS.glsl");
	    depthFB = new FrameBuffer();
	    depthTex  = new Texture(GL_TEXTURE_2D, textureUnit++);
	    depth2Tex = new Texture(GL_TEXTURE_2D, textureUnit++);
	    depth3Tex = new Texture(GL_TEXTURE_2D, textureUnit++);
    	
	    initFrameBuffer(depthFB, depthSP, new Texture[]{depthTex}, new String[]{"depth"}, false, true);
	    depthSP.use();
	    initParticles();
	}
	
	
	private void initFrameBuffer(FrameBuffer fb, ShaderProgram sp, Texture[] textures, String[] names, boolean low, boolean depthTest) {
		fb.init(depthTest, WIDTH/(low?2:1), HEIGHT/(low?2:1));
		for(Texture tex: textures) {
			fb.addTexture(tex, GL_RGBA16F, GL_RGBA);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		fb.drawBuffers();

		fb.bind();
		fb.clearColor();
		for(int i = 0; i < names.length; i++) {
			glBindFragDataLocation(sp.getId(), i, names[i]);
		}
		

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	
	private void initParticles() {
		glBindVertexArray(vertexArrayID);
		bufferObjectPositions = glGenBuffers();

		// generate particles
		for(int i = 0; i < elements; i++) {
			ParticleFactory.createParticle();
		}

		FloatBuffer particlePositions = ParticleFactory.getParticlePositions();

		bufferObjectPositions = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bufferObjectPositions);
		glBufferData(GL_ARRAY_BUFFER, particlePositions, GL_STATIC_DRAW);
		
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3 * SizeOf.FLOAT, 0);
        
        FloatBuffer particleLifetimes = ParticleFactory.getParticleLifetime();
        
        bufferObjectLifetimes = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferObjectLifetimes);
        glBufferData(GL_ARRAY_BUFFER, particleLifetimes, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(ShaderProgram.ATTR_NORMAL);
        glVertexAttribPointer(ShaderProgram.ATTR_NORMAL, 2, GL_FLOAT, false, 2 * SizeOf.FLOAT, 0);
        
        
        
        
	}

	public void run() {
		System.out.println("Running with " + elements + " Particles.");
		
		// push OpenGL Buffer to OpenCL
		memPositions = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectPositions);
		memLifetime = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectLifetimes);
		
        OpenCL.clSetKernelArg(kernelMove, 0, memPositions);
        OpenCL.clSetKernelArg(kernelMove, 1, memLifetime);
        
        // calculate global work size
		PointerBuffer gws = new PointerBuffer(elements);
        gws.put(0, elements);
        
		while(running) {
			long deltaTime = System.currentTimeMillis() - lastTimestamp;
			calculateFramesPerSecond(deltaTime);
			
			handleInput(deltaTime);
			
			OpenCL.clSetKernelArg(kernelMove, 2, (int)deltaTime);
			
			
			OpenCL.clEnqueueAcquireGLObjects(queue, memPositions, null, null);
			OpenCL.clEnqueueAcquireGLObjects(queue, memLifetime, null, null);
	        OpenCL.clEnqueueNDRangeKernel(queue, kernelMove, 1, null, gws, null, null, null);
	        OpenCL.clEnqueueReleaseGLObjects(queue, memLifetime, null, null);
	        OpenCL.clEnqueueReleaseGLObjects(queue, memPositions, null, null);
	        
//	        debugCL(memPositions, 3, 1);
//	        debugCL(memLifetime, 2, 5);
		
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				stop();
			}
		}
	}
	
	public void initCL() {

		CLUtil.createCL();
		
		PlatformDevicePair pair = CLUtil.choosePlatformAndDevice();
		
		context = OpenCL.clCreateContext(pair.platform, pair.device, null, Display.getDrawable());
        
		queue = OpenCL.clCreateCommandQueue(context, pair.device, OpenCL.CL_QUEUE_PROFILING_ENABLE);
		// for out of order queue: OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        
        program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        kernelMove = OpenCL.clCreateKernel(program, "move");
        
	}
	
	public void stop() {
		running = false;
		
//		shaderProgram.delete();
		
		if(!Display.isCloseRequested())  {
			Display.destroy();
		}
		
		OpenCL.clReleaseMemObject(memLifetime);
        OpenCL.clReleaseMemObject(memPositions);
        OpenCL.clReleaseKernel(kernelMove);
        OpenCL.clReleaseProgram(program);
        OpenCL.clReleaseCommandQueue(queue);
        OpenCL.clReleaseContext(context);
        
        CLUtil.destroyCL();
        
		GL.destroy();
	}
	
	public void initGL() {
		try {
			GL.init();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
//		shaderProgram = new ShaderProgram("shader/DefaultVS.glsl", "shader/DefaultFS.glsl");
		
		glClearColor(0.1f, 0.0f, 0.4f, 1.0f);
		
		vertexArrayID = glGenVertexArrays();
		glBindVertexArray(vertexArrayID);
	}
	
	public void drawScene() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
//		shaderProgram.use();
//		shaderProgram.setUniform("model", modelMat);
////		shaderProgram.setUniform("modelIT", modelIT);
//		shaderProgram.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
//		shaderProgram.setUniform("camPos", cam.getCamPos());


//        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4);
//        floatBuffer.put(new float[]{1.0f, 1.0f, 0.0f, 0.0f});
//        floatBuffer.position(0);
//        glPointParameter(GL_POINT_DISTANCE_ATTENUATION, floatBuffer);
//
//		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		depthSP.use();
		depthSP.setUniform("model", modelMat);
		depthSP.setUniform("view", cam.getView());
		depthSP.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
		depthSP.setUniform("viewDistance", 1e+2f);
        depthSP.setUniform("camPos", cam.getCamPos());
        depthSP.setUniform("size", 20.0f);

        depthFB.bind();
        depthFB.clearColor();
        glBindVertexArray(vertexArrayID);
        ParticleFactory.draw();

//        glDisable(GL_BLEND);
//        glEnable(GL_DEPTH_TEST);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
        

        drawTextureSP.use();        
		drawTextureSP.setUniform("image", depthTex);
		screenQuad.draw();
        
		
		
		
        // present screen
        Display.update();
        Display.sync(60);
	}
	
	private void calculateFramesPerSecond(long deltaTime) {
		numberOfFrames++;
        if(deltaTime > 1000) {
        	float fps = numberOfFrames / (float)(deltaTime / 1000);
        	lastTimestamp  = System.currentTimeMillis();
        	numberOfFrames = 0;
        	Display.setTitle("FPS: " + fps);
        }
	}
	
	public void debugCL(CLMem memObject, int numberOfValues) {
		debugCL(memObject, numberOfValues, 3);
	}
	
	public void debugCL(CLMem memObject, int numberOfValues, int maxParticles) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(elements * numberOfValues);
		OpenCL.clEnqueueAcquireGLObjects(queue, memObject, null, null);
        OpenCL.clEnqueueReadBuffer(queue, memObject, 0, 0, fb, null, null);
        OpenCL.clEnqueueReleaseGLObjects(queue, memObject, null, null);
        fb.rewind();

        for(int i = 0; i < Math.min(fb.capacity(), maxParticles * numberOfValues); i++) {
        	if(i%numberOfValues == 0)
        		System.out.print("Particle " + i/numberOfValues + ": ");
        	System.out.print(fb.get(i) + ((i%numberOfValues == numberOfValues-1)?"\n":", "));
        }
        System.out.println();
	}
	
	
	private void handleInput(long deltaTime) {
		float speed = 5e-6f * deltaTime;
			
		while(Mouse.next()) {
            if(Mouse.isButtonDown(0)) {
                cam.rotate(-speed*Mouse.getEventDX(), -speed*Mouse.getEventDY());
            }
        }
	}
}
