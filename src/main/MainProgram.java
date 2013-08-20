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
	private int bufferObject = -1;
	private int elements = 1<<8; // 2^n = 1<<n 

	////// OPENCL BLOCK
	private CLContext context;
	private CLCommandQueue queue;
	private CLProgram program;
	private CLKernel kernel;
	private CLMem mem;

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
		bufferObject = glGenBuffers();
		

		// generate particles
		for(int i = 0; i < elements; i++) {
			ParticleFactory.createParticle();
		}
		FloatBuffer particleData = ParticleFactory.getParticleData();
		
		glBindBuffer(GL_ARRAY_BUFFER, bufferObject);
		glBufferData(GL_ARRAY_BUFFER, particleData, GL_STATIC_DRAW);
		
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3*4, 0);
        
        
        
        
	}

	public void run() {
		System.out.println("Running with " + elements + " Particles.");
		
		// push OpenGL Buffer to OpenCL
		mem = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObject);
        OpenCL.clSetKernelArg(kernel, 0, mem);
        
        // calculate global work size
		PointerBuffer gws = new PointerBuffer(elements);
        gws.put(0, elements);
        
		while(running) {
			long deltaTime = System.currentTimeMillis() - lastTimestamp;
			calculateFramesPerSecond(deltaTime);
			handleInput(deltaTime);
	        
			OpenCL.clEnqueueAcquireGLObjects(queue, mem, null, null);
	        OpenCL.clEnqueueNDRangeKernel(queue, kernel, 1, null, gws, null, null, null);
	        OpenCL.clEnqueueReleaseGLObjects(queue, mem, null, null);
	        
//			debugCL(mem);	        
//	        debugCL(mem, 1);
//	        debugCL(mem, 5);
		
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
        
        kernel = OpenCL.clCreateKernel(program, "move");
        
	}
	
	public void stop() {
		running = false;
		
		if(!Display.isCloseRequested())  {
			Display.destroy();
		}
		
        OpenCL.clReleaseMemObject(mem);
        OpenCL.clReleaseKernel(kernel);
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
	
	public void debugCL(CLMem memObject) {
		debugCL(memObject, 3);
	}
	
	public void debugCL(CLMem memObject, int maxParticles) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(elements * Particle.getNumberOfFloatValues());
		OpenCL.clEnqueueAcquireGLObjects(queue, memObject, null, null);
        OpenCL.clEnqueueReadBuffer(queue, memObject, 0, 0, fb, null, null);
        OpenCL.clEnqueueReleaseGLObjects(queue, memObject, null, null);
        fb.rewind();

        for(int i = 0; i < Math.min(fb.capacity(), maxParticles * Particle.getNumberOfFloatValues()); i++) {
        	if(i%Particle.getNumberOfFloatValues() == 0)
        		System.out.print("Particle " + i/Particle.getNumberOfFloatValues() + ": ");
        	System.out.print(fb.get(i) + ((i%Particle.getNumberOfFloatValues() == Particle.getNumberOfFloatValues()-1)?"\n":", "));
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
