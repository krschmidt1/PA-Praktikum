package main;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;

import pa.cl.CLUtil;
import pa.cl.OpenCL;
import pa.cl.CLUtil.PlatformDeviceFilter;
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

import particle.ParticleFactory;
import opengl.util.FrameBuffer;
import opengl.util.Texture;
import opengl.util.Geometry;
import opengl.util.GeometryFactory;

public class MainProgram {
	private boolean running = true;

	////// PARAMETERS
	private int elements         = 1<<16; // 2^n = 1<<n // we want 1<<16 
	private int spawnElements    = 1<<7;  // we want 1<< 5-7
	private long respawnInterval = 100; // milliseconds
	private int numberLPA        = 1<<5; // number of low pressure areas
	
	////// SHARED BLOCK
	private int bufferObjectPositions  = -1;
	private int bufferObjectLifetimes  = -1;
	private int bufferObjectVelocities = -1;

	////// OPENCL BLOCK
	private CLContext context    = null;
	private CLCommandQueue queue = null;
	private CLCommandQueue oooQueue = null;
	private CLProgram program    = null;
	private CLKernel kernelMove  = null;
	private CLKernel kernelShift  = null;
	private CLKernel kernelSpawn = null;
    private CLKernel kernelBitonic     = null;
    private CLKernel kernelBitonicUp   = null;
    private CLKernel kernelBitonicDown = null;
	private CLMem memPositions   = null;
	private CLMem memVelocities  = null;
	private CLMem memLifetimes    = null;
	private CLMem memNewParticles = null;
	private CLMem memLowPressureAreas = null;

	////// OPENGL BLOCK + DEFERRED SHADING
	private Matrix4f modelMat = new Matrix4f();
	private Camera   cam      = new Camera();
	private int vertexArrayID = -1;
	
	private Geometry screenQuad = null;
	private ShaderProgram screenQuadSP = null;

	private int textureUnit = 0;
	private ShaderProgram depthSP = null;
	private FrameBuffer depthFB   = null;
	private Texture depthTex      = null;

	////// other
	private long lastTimestamp  = System.currentTimeMillis();
	private long sumDeltaTime   = 0;
	private int  numberOfFrames = 0;
	private long respawnTimer   = respawnInterval;
	
	private boolean animating = true;

	
	public MainProgram() {
	    initGL();
		initCL();
	    initParticleBuffers();
	}
	
	private void initParticleBuffers() {
	    // vertex array for particles (the screen quad uses a different one)
	    // TODO: both in one vertexarray?
	    vertexArrayID = glGenVertexArrays();
        glBindVertexArray(vertexArrayID);

		// positions
		FloatBuffer particlePositions = ParticleFactory.createZeroFloatBuffer(elements * 3);
		bufferObjectPositions = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, bufferObjectPositions);
		glBufferData(GL_ARRAY_BUFFER, particlePositions, GL_STATIC_DRAW);
		
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3 * SizeOf.FLOAT, 0);
        
        // velocities
        FloatBuffer particleVelocities = ParticleFactory.createZeroFloatBuffer(elements * 3);
        bufferObjectVelocities = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferObjectVelocities);
        glBufferData(GL_ARRAY_BUFFER, particleVelocities, GL_STATIC_DRAW);
        
        // lifetimes
        FloatBuffer particleLifetimes = ParticleFactory.createZeroFloatBuffer(elements * 2);
        bufferObjectLifetimes = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferObjectLifetimes);
        glBufferData(GL_ARRAY_BUFFER, particleLifetimes, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(ShaderProgram.ATTR_NORMAL);
        glVertexAttribPointer(ShaderProgram.ATTR_NORMAL, 2, GL_FLOAT, false, 2 * SizeOf.FLOAT, 0);
        
        // pressure areas
        FloatBuffer pressureAreas = ParticleFactory.createLPA(numberLPA);
        memLowPressureAreas = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_WRITE, pressureAreas);
        
	}

	public void run() {
		System.out.println("Running with " + elements + " Particles.");
		
		// push OpenGL Buffer to OpenCL TODO
		memPositions  = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectPositions);
		memVelocities = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectVelocities);
		memLifetimes  = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectLifetimes);
		
		// static kernel arguments
        OpenCL.clSetKernelArg(kernelMove, 0, memPositions);
        OpenCL.clSetKernelArg(kernelMove, 1, memVelocities);
        OpenCL.clSetKernelArg(kernelMove, 2, memLifetimes);
        OpenCL.clSetKernelArg(kernelMove, 3, memLowPressureAreas);
        OpenCL.clSetKernelArg(kernelMove, 4, numberLPA);

        OpenCL.clSetKernelArg(kernelShift, 0, memPositions);
        OpenCL.clSetKernelArg(kernelShift, 1, memVelocities);
        OpenCL.clSetKernelArg(kernelShift, 2, memLifetimes);
        
        OpenCL.clSetKernelArg(kernelSpawn, 0, memPositions);
        OpenCL.clSetKernelArg(kernelSpawn, 1, memVelocities);
        OpenCL.clSetKernelArg(kernelSpawn, 2, memLifetimes);
        
        OpenCL.clSetKernelArg(kernelBitonic,     0, memLifetimes);
        OpenCL.clSetKernelArg(kernelBitonicUp,   0, memLifetimes);
        OpenCL.clSetKernelArg(kernelBitonicDown, 0, memLifetimes);
        OpenCL.clSetKernelArg(kernelBitonic,     1, memPositions);
        OpenCL.clSetKernelArg(kernelBitonicUp,   1, memPositions);
        OpenCL.clSetKernelArg(kernelBitonicDown, 1, memPositions);
        OpenCL.clSetKernelArg(kernelBitonic,     2, memVelocities);
        OpenCL.clSetKernelArg(kernelBitonicUp,   2, memVelocities);
        OpenCL.clSetKernelArg(kernelBitonicDown, 2, memVelocities);
        
        // calculate global work size
		PointerBuffer gws = new PointerBuffer(elements);
        gws.put(0, elements);
        
        spawnElements = Math.min(spawnElements, elements);
        int numberOfParticleProperties = 3 + 3 + 1;
        FloatBuffer bufferNewParticleData = BufferUtils.createFloatBuffer(spawnElements * numberOfParticleProperties);
        System.out.println("Respawning: " + spawnElements + " elements per " + respawnInterval + " ms.");
        
        while(running) {
			long deltaTime = System.currentTimeMillis() - lastTimestamp;
			lastTimestamp += deltaTime;
			respawnTimer  += deltaTime;
			calculateFramesPerSecond(deltaTime);
			
			handleInput(deltaTime);
			
			// TODO
			if(animating) {
			    
    			OpenCL.clEnqueueAcquireGLObjects(queue, memPositions, null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memVelocities, null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memLifetimes, null, null);
    
    			OpenCL.clSetKernelArg(kernelMove, 5, (int)deltaTime);
    			OpenCL.clEnqueueNDRangeKernel(queue, kernelMove, 1, null, gws, null, null, null);
    	        
    	        if(respawnTimer >= respawnInterval) {
    	            respawnTimer = 0;
    	            
        	        for(int i = 0; i < spawnElements * numberOfParticleProperties; i += numberOfParticleProperties) {
        	            int j = 0;
                        float[] pos  = ParticleFactory.generateCoordinates();
                        float[] velo = ParticleFactory.generateVelocity();
                        bufferNewParticleData.put(i + j++, pos[0]);
                        bufferNewParticleData.put(i + j++, pos[1]);
                        bufferNewParticleData.put(i + j++, pos[2]);
                        bufferNewParticleData.put(i + j++, velo[0]);
                        bufferNewParticleData.put(i + j++, velo[1]);
                        bufferNewParticleData.put(i + j++, velo[2]);
                        bufferNewParticleData.put(i + j++, ParticleFactory.generateLifetime());
                    }
                    
        	        shiftParticles();
        	        
                    if(memNewParticles != null) {
                        OpenCL.clReleaseMemObject(memNewParticles);
                        memNewParticles = null;
                    }
                    memNewParticles = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, bufferNewParticleData);
                    

                    gws.put(0, spawnElements);
                    OpenCL.clSetKernelArg(kernelSpawn, 3, memNewParticles);
                    OpenCL.clEnqueueNDRangeKernel(queue, kernelSpawn, 1, null, gws, null, null, null);
                    gws.put(0, elements);
    	        }
    	        
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memLifetimes,   null, null);
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memVelocities, null, null);
                OpenCL.clEnqueueReleaseGLObjects(queue, memPositions,  null, null);
                
			}  // if animating
	        
//	        debugCL(memPositions, 3, 1);
//	        debugCL(memLifetime, 2, 5);
		
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				stop();
			}
		}
		System.out.println("Program shut down properly.");
	}
	
	
	private void shiftParticles() {
		PointerBuffer gws = new PointerBuffer(1);
        //gws.put(0, elements-spawnElements);
        gws.put(0, 1);
        OpenCL.clSetKernelArg(kernelShift, 4, spawnElements);
        OpenCL.clSetKernelArg(kernelShift, 3, elements-spawnElements);
        
        OpenCL.clEnqueueNDRangeKernel(queue, kernelShift, 1, null, gws, null, null, null);
	}
	
	
	/**
     * 
     */
    private void sortParticles(int n) {
        PointerBuffer gws = new PointerBuffer(1);
        gws.put(0, n / 2);
        
        int logN = (int)(Math.log(n) / Math.log(2));
        int kernelCount = n / 2; 
        int phase   = 1;
        int offset1 = 0;
        int offset2 = 0;
        int runs    = 0;
        
        for(int i = 0; i < logN; i++) {
            runs++;
            for(int j= 0; j < kernelCount/2; j++) {
                offset1 = j * (n/kernelCount) * 2;
                offset2 = j * (n/kernelCount) * 2 + (n/kernelCount);
                phase = 1;
                for(int k = 0; k < runs; k++) {
                    gws.put(0, (n / 2) / kernelCount);
                    OpenCL.clSetKernelArg(kernelBitonicUp,   3, phase);
                    OpenCL.clSetKernelArg(kernelBitonicDown, 3, phase);
                    OpenCL.clSetKernelArg(kernelBitonicUp,   4, offset1);
                    OpenCL.clSetKernelArg(kernelBitonicDown, 4, offset2);
                    
                    OpenCL.clEnqueueNDRangeKernel(oooQueue, kernelBitonicUp,   1, null, gws, null, null, null);
                    OpenCL.clEnqueueNDRangeKernel(oooQueue, kernelBitonicDown, 1, null, gws, null, null, null);
                    
                    phase *= 2;
                }
                OpenCL.clFinish(oooQueue);
            }
            kernelCount /= 2;
        }
        
        gws.put(0, n / 2);
        phase = 1;
        for(int i = 0; i < logN; i++) {
            OpenCL.clSetKernelArg(kernelBitonic, 3, phase);
            OpenCL.clEnqueueNDRangeKernel(oooQueue, kernelBitonic, 1, null, gws, null, null, null);
            OpenCL.clFinish(oooQueue);
            phase *= 2;
        }
    }

    public void drawScene() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// post effects etc
		depthSP.use();
		depthSP.setUniform("model", modelMat);
		depthSP.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
        depthSP.setUniform("camPos", cam.getCamPos());

        depthFB.bind();
        depthFB.clearColor();
        
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        
        glBindVertexArray(vertexArrayID);
        opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, elements);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
		
		
		// draw texture on screenquad
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
		screenQuadSP.use();        
		screenQuadSP.setUniform("image", depthTex);
		screenQuad.draw();
		
        // present screen
        Display.update();
//        Display.sync(60);
	}
	
	private void handleInput(long deltaTime) {
        float speed = 5e-6f * deltaTime;
        
        if(Keyboard.next() && Keyboard.isKeyDown(Keyboard.getEventKey())) {
            switch(Keyboard.getEventKey()) {
                case Keyboard.KEY_S: animating = !animating; 
                    break; 
            }
        }
        
        while(Mouse.next()) {
            if(Mouse.isButtonDown(0)) {
                cam.rotate(-speed*Mouse.getEventDX(), -speed*Mouse.getEventDY());
            }
        }
    }
	
	public void initGL() {
        try {
            GL.init();
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        
        // screenQuad
        screenQuad   = GeometryFactory.createScreenQuad();
        screenQuadSP = new ShaderProgram("shader/ScreenQuad_VS.glsl", "shader/CopyTexture_FS.glsl");
        
        // first renderpath: "depth"
        depthSP = new ShaderProgram("./shader/DefaultVS.glsl", "./shader/Default1FS.glsl");
        depthSP.use();
        
        depthFB = new FrameBuffer();
        depthFB.init(true, WIDTH, HEIGHT);

        depthTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        depthFB.addTexture(depthTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(depthSP.getId(), 0, "depth");
        
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glBlendFunc(GL_ONE, GL_ONE);
    }
	
	public void initCL() {
        CLUtil.createCL();
        
        PlatformDevicePair pair = null;
        try {
            PlatformDeviceFilter filter = new PlatformDeviceFilter();
            
            // set spec here
            filter.addPlatformSpec(CL10.CL_PLATFORM_VENDOR, "NVIDIA");
            filter.setDesiredDeviceType(CL10.CL_DEVICE_TYPE_GPU);
                
            // query platform and device
            pair = CLUtil.choosePlatformAndDevice(filter);
        }catch(Exception e) {
            pair = CLUtil.choosePlatformAndDevice();
        }
        
        context  = OpenCL.clCreateContext(pair.platform, pair.device, null, Display.getDrawable());
        queue    = OpenCL.clCreateCommandQueue(context, pair.device, 0);
        oooQueue = OpenCL.clCreateCommandQueue(context, pair.device, OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE);
        // for out of order queue: OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        
        program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        kernelMove  = OpenCL.clCreateKernel(program, "move");
        kernelShift = OpenCL.clCreateKernel(program, "shift");
        kernelSpawn = OpenCL.clCreateKernel(program, "respawn");
        // TODO other kernels
        kernelBitonic     = OpenCL.clCreateKernel(program, "bitonicSort");
        kernelBitonicUp   = OpenCL.clCreateKernel(program, "makeBitonicUp");
        kernelBitonicDown = OpenCL.clCreateKernel(program, "makeBitonicDown");
    }
	
	public void stop() {
	    // TODO: Nullchecks
        running = false;
        
        screenQuadSP.delete();
        depthSP.delete();
        // TODO cleanup (possible) additional sps
        
        if(!Display.isCloseRequested())  {
            Display.destroy();
        }
        
        OpenCL.clReleaseMemObject(memLifetimes);
        OpenCL.clReleaseMemObject(memVelocities);
        OpenCL.clReleaseMemObject(memPositions);
        OpenCL.clReleaseMemObject(memLowPressureAreas);
        
        if(memNewParticles != null)
            OpenCL.clReleaseMemObject(memNewParticles);
        
        OpenCL.clReleaseKernel(kernelSpawn);
        OpenCL.clReleaseKernel(kernelShift);
        OpenCL.clReleaseKernel(kernelMove);
        
        // TODO SORT
        OpenCL.clReleaseKernel(kernelBitonicDown);
        OpenCL.clReleaseKernel(kernelBitonicUp);
        OpenCL.clReleaseKernel(kernelBitonic);

        OpenCL.clReleaseProgram(program);
        OpenCL.clReleaseCommandQueue(oooQueue);
        OpenCL.clReleaseCommandQueue(queue);
        OpenCL.clReleaseContext(context);
        
        CLUtil.destroyCL();
        
        GL.destroy();
    }
	
	private void calculateFramesPerSecond(long deltaTime) {
		numberOfFrames++;
		sumDeltaTime += deltaTime;
        if(sumDeltaTime > 1000) {
        	float fps = numberOfFrames / (float)(sumDeltaTime / 1000);
        	numberOfFrames = 0;
        	sumDeltaTime   = 0;
        	Display.setTitle("FPS: " + fps);
        }
	}
	
	public void debugCL(CLMem memObject, int numberOfValues) {
		debugCL(memObject, numberOfValues, 3);
	}
	
	public void debugCL(CLMem memObject, int numberOfValues, int maxParticles) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(elements * numberOfValues);
        OpenCL.clEnqueueReadBuffer(queue, memObject, 0, 0, fb, null, null);
        fb.rewind();

        for(int i = 0; i < Math.min(fb.capacity(), maxParticles * numberOfValues); i++) {
        	if(i%numberOfValues == 0)
        		System.out.print("Particle " + i/numberOfValues + ": ");
        	System.out.print(fb.get(i) + ((i%numberOfValues == numberOfValues-1)?"\n":", "));
        }
        System.out.println();
	}
	
	
}
