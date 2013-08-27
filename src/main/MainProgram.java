package main;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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
import org.lwjgl.util.vector.Vector3f;

import particle.ParticleFactory;
import opengl.util.FrameBuffer;
import opengl.util.Texture;
import opengl.util.Geometry;
import opengl.util.GeometryFactory;

public class MainProgram {
	private boolean running = true;

	////// PARAMETERS
	private int elements           = 1<<17; // 2^n = 1<<n // we want 1<<16 
	private int spawnElements      = 1<<10;  // we want 1<< 5-7
	private long respawnInterval   = 100; // milliseconds
	private long changeLPAInterval = 500;
	private int numberLPA          = 1<<5; // number of low pressure areas
	
	////// SHARED BLOCK
	private int bufferObjectPositions  = -1;
	private int bufferObjectLifetimes  = -1;
	private int bufferObjectVelocities = -1;

	////// OPENCL BLOCK
	private CLContext context     = null;
	private CLCommandQueue queue  = null;
	private CLProgram program     = null;
	private CLKernel kernelMove   = null;
	private CLKernel kernelSpawn  = null;
	private CLMem memPositions    = null;
	private CLMem memVelocities   = null;
	private CLMem memLifetimes    = null;
	private CLMem memNewParticles = null;
	private CLMem memLPAs         = null;
	private CLMem memLPARandoms   = null;

	////// OPENGL BLOCK + DEFERRED SHADING
	private Matrix4f modelMat  = new Matrix4f();
	private Camera   cam       = new Camera();
	private int vertexArrayID  = -1;
	
	private Geometry screenQuad        = null;
	private ShaderProgram screenQuadSP = null;

	private int textureUnit       = 0;
	private ShaderProgram depthSP = null;
	private FrameBuffer depthFB   = null;
	private Texture depthTex      = null;

	////// other
	private long lastTimestamp  = System.currentTimeMillis();
	private long sumDeltaTime   = 0;
	private int  numberOfFrames = 0;
	private long respawnTimer   = respawnInterval;
	private long changeLPATimer = changeLPAInterval; 
	private int  spawnOffset    = 0;
	
	private boolean showLPA   = false;
	private boolean animating = true;
	private boolean debug     = false;
	
	// TODO dirty hack
	private boolean pulse = false;
	
	public MainProgram() {
	    initGL();
		initCL();
	    initParticleBuffers();
	}
	
	private void initParticleBuffers() {
	    // vertex array for particles (the screen quad uses a different one)
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
	}

	public void run() {
		System.out.println("Running with " + elements + " Particles.");
		
		memPositions  = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectPositions);
		memVelocities = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectVelocities);
		memLifetimes  = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObjectLifetimes);
		
		// static kernel arguments
        OpenCL.clSetKernelArg(kernelMove, 0, memPositions);
        OpenCL.clSetKernelArg(kernelMove, 1, memVelocities);
        OpenCL.clSetKernelArg(kernelMove, 2, memLifetimes);
        OpenCL.clSetKernelArg(kernelMove, 5, numberLPA);

        OpenCL.clSetKernelArg(kernelSpawn, 0, memPositions);
        OpenCL.clSetKernelArg(kernelSpawn, 1, memVelocities);
        OpenCL.clSetKernelArg(kernelSpawn, 2, memLifetimes);
        
        // calculate global work size
		PointerBuffer gws = new PointerBuffer(elements);
        gws.put(0, elements);

        // limit respawn elements to elements, create the buffer
        spawnElements = Math.min(spawnElements, elements);
        int numberOfParticleProperties = 3 + 3 + 1;
        FloatBuffer bufferNewParticleData = BufferUtils.createFloatBuffer(spawnElements * numberOfParticleProperties);
        System.out.println("Respawning " + spawnElements + " elements per " + respawnInterval + " ms.");
        
        // create indices for LPA seeking
        IntBuffer bufferRandIndices = BufferUtils.createIntBuffer(elements);
        System.out.println("Using " + numberLPA + " low pressure areas.");

        // spawn first LPAs
        
        FloatBuffer pressureAreas;
        if(debug) {
			numberLPA = 30;
			pressureAreas = ParticleFactory.createOrderedLPA();
		} else { 
			pressureAreas = ParticleFactory.createLPA(numberLPA);
		}
        memLPAs = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_WRITE, pressureAreas);
        
        while(running) {
			long deltaTime = System.currentTimeMillis() - lastTimestamp;
			lastTimestamp  += deltaTime;
			respawnTimer   += deltaTime;
			changeLPATimer += deltaTime;
			calculateFramesPerSecond(deltaTime);
			
			handleInput(deltaTime);
			
			if(animating) {
				// ACQUIRE OPENGL BUFFERS
    			OpenCL.clEnqueueAcquireGLObjects(queue, memPositions,  null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memVelocities, null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memLifetimes,  null, null);
    			
    			
    			
    			// SET RANDOM PARAMS
    			if(changeLPATimer >= changeLPAInterval) {
    	        	changeLPATimer = 0;
    	        	int[] a = new int[4];
                    for(int i = 0; i < elements; i++) {
                    	int id = (int)((numberLPA * numberLPA * ParticleFactory.lifetime() + numberLPA * ParticleFactory.lifetime() + numberLPA)) % 4;
                    	a[id]+=1;
                    	bufferRandIndices.put(i, id);
                    }
                    
                    if(memLPARandoms != null) {
                    	OpenCL.clReleaseMemObject(memLPARandoms);
                    	memLPARandoms = null;
                    }
                    memLPARandoms = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, bufferRandIndices);
                    
                    
                    
                    // MOVE LPA
        			if(!debug) {
        				pressureAreas = ParticleFactory.createLPA(numberLPA);
    	                if(memLPAs != null) {
    	                    OpenCL.clReleaseMemObject(memLPAs);
    	                    memLPAs = null;
    	                }
    	                memLPAs = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, pressureAreas);
        			}
                    if(showLPA) {
    	                glBindBuffer(GL_ARRAY_BUFFER, bufferObjectPositions);
    	        		glBufferData(GL_ARRAY_BUFFER, pressureAreas, GL_STATIC_DRAW);
                    }
    	        }
    			
    			
    			
    			// MOVE PARTICLES
    			gws.put(0, elements);
    	        OpenCL.clSetKernelArg(kernelMove, 3, memLPAs);
    			OpenCL.clSetKernelArg(kernelMove, 4, memLPARandoms);
    			OpenCL.clSetKernelArg(kernelMove, 6, (int)deltaTime);
    			
    			// TODO dirty hack to test
    			OpenCL.clSetKernelArg(kernelMove, 7, pulse?1:0);
    			if(pulse) pulse=false;
    			
    			OpenCL.clEnqueueNDRangeKernel(queue, kernelMove, 1, null, gws, null, null, null);

    			
    			// RESPAWN
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
                        bufferNewParticleData.put(i + j++, ParticleFactory.lifetime());
                    }
                    
                    if(memNewParticles != null) {
                        OpenCL.clReleaseMemObject(memNewParticles);
                        memNewParticles = null;
                    }
                    memNewParticles = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, bufferNewParticleData);
                    
                    gws.put(0, spawnElements);
                    OpenCL.clSetKernelArg(kernelSpawn, 3, memNewParticles);
                    OpenCL.clSetKernelArg(kernelSpawn, 4, elements);
                    OpenCL.clSetKernelArg(kernelSpawn, 5, spawnOffset);
                    OpenCL.clEnqueueNDRangeKernel(queue, kernelSpawn, 1, null, gws, null, null, null);

                    spawnOffset = (spawnOffset + spawnElements) % elements;
    	        }


    			
    			// FREE OPENGL BUFFERS
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memLifetimes,  null, null);
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memVelocities, null, null);
                OpenCL.clEnqueueReleaseGLObjects(queue, memPositions,  null, null);
                
			}  // if animating
		
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				stop();
			}
		}
		System.out.println("Program shut down properly.");
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
    
	private Vector3f moveDir = new Vector3f(0.0f,0.0f,0.0f);
	
	private void handleInput(long deltaTime) {
        float speed = 1e-3f * deltaTime;
        float moveSpeed = 1e-3f * (float)deltaTime;

        while(Keyboard.next()) {
            if(Keyboard.getEventKeyState()) {
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W: moveDir.z += 1.0f; break;
                    case Keyboard.KEY_S: moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_A: moveDir.x += 1.0f; break;
                    case Keyboard.KEY_D: moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y += 1.0f; break;
                    case Keyboard.KEY_C: moveDir.y -= 1.0f; break;
                }
            } else {
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W: moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_S: moveDir.z += 1.0f; break;
                    case Keyboard.KEY_A: moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_D: moveDir.x += 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y -= 1.0f; break;
                    case Keyboard.KEY_C: moveDir.y += 1.0f; break;
                    case Keyboard.KEY_E: animating = !animating; break;
                    case Keyboard.KEY_L: showLPA = !showLPA; break;
                    case Keyboard.KEY_H: debug = !debug; break;
                    // TODO dirty hack
                    case Keyboard.KEY_P: pulse = true; break;
                }
            }
        }
        
        cam.move(moveSpeed * moveDir.z, moveSpeed * moveDir.x, moveSpeed * moveDir.y);
        
        while(Mouse.next()) {
            if(Mouse.isButtonDown(0)) {
                cam.rotate(-speed*Mouse.getEventDX(), -speed*Mouse.getEventDY());
            }
        }
    }
	
	private void initGL() {
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
	
	private void initCL() {
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
        // for out of order queue: OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        
        program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        kernelMove  = OpenCL.clCreateKernel(program, "move");
        kernelSpawn = OpenCL.clCreateKernel(program, "respawn");

    }
	
	private void stop() {
        running = false;
        
        // Shaderprograms
        if(screenQuadSP != null)
        	screenQuadSP.delete();
        if(depthSP != null)
        	depthSP.delete();
        
        // Display
        if(!Display.isCloseRequested())  {
            Display.destroy();
        }
        
        // MemObjects
        if(memLPARandoms != null)
        	OpenCL.clReleaseMemObject(memLPARandoms);
        if(memLifetimes != null)
        	OpenCL.clReleaseMemObject(memLifetimes);
        if(memVelocities != null)
        	OpenCL.clReleaseMemObject(memVelocities);
        if(memPositions != null)
        	OpenCL.clReleaseMemObject(memPositions);
        if(memLPAs != null)
        	OpenCL.clReleaseMemObject(memLPAs);
        if(memNewParticles != null)
            OpenCL.clReleaseMemObject(memNewParticles);

        // Kernels
        if(kernelSpawn != null)
        	OpenCL.clReleaseKernel(kernelSpawn);
        if(kernelMove != null)
        	OpenCL.clReleaseKernel(kernelMove);
        
        // OpenCL Context
        if(program != null)
        	OpenCL.clReleaseProgram(program);
        if(queue != null)
        	OpenCL.clReleaseCommandQueue(queue);
        if(context != null)
        	OpenCL.clReleaseContext(context);
        
        // OpenCL and OpenGL
        CLUtil.destroyCL();
        GL.destroy();
    }
	
	/**
	 * calculates FPS
	 * @param deltaTime
	 */
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
	
}
