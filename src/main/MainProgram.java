package main;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
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
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import particle.ParticleFactory;
import opengl.util.FrameBuffer;
import opengl.util.Texture;
import opengl.util.Geometry;
import opengl.util.GeometryFactory;

public class MainProgram {
    private boolean running = true;

	////// PARAMETERS
	private int  elements          = 1<<16; // we want 1<<17
	private int  defaultSpawn      = 1<<5;  // we want 1<<7   
	private long changeLPAInterval = 1<<7;  // we want 1<<7
	private int  numberLPA         = 1<<6;  // we want 1<<6
	private long mouseThreshold    = 200;
	
	////// SHARED BLOCK
	private int bufferObjectPositions  = -1;
	private int bufferObjectLifetimes  = -1;
	private int bufferObjectVelocities = -1;
	private int bufferObjectLPA        = -1;

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

    private int lpaVAID         = -1;
    private ShaderProgram lpaSP = null;
    
    private Geometry screenQuad        = null;
    private ShaderProgram screenQuadSP = null;

    private int textureUnit       = 0;
    private ShaderProgram depthSP = null;
    private ShaderProgram glowSP  = null;
    private ShaderProgram blurSP  = null;
    private ShaderProgram finalSP  = null;
    private FrameBuffer depthFB = null;
    private FrameBuffer glowFB  = null;
    private FrameBuffer hBlurFB = null;
    private FrameBuffer vBlurFB = null;
    private FrameBuffer finalFB = null;
    private Texture depthTex = null;
    private Texture glowTex  = null;
    private Texture noiseTex = null;
    private Texture hBlurTex = null;
    private Texture vBlurTex = null;
    private Texture finalTex = null;

	////// other
	private long lastTimestamp   = System.currentTimeMillis();
	private long sumDeltaTime    = 0;
	private int  numberOfFrames  = 0;
	private float fps            = 0;
	private long changeLPATimer  = changeLPAInterval; 
	private int  spawnOffset     = 0;
	private int  spawnElements   = defaultSpawn;
	private long mouseDelay      = 0;
	private Vector3f moveDir     = new Vector3f(0.0f,0.0f,0.0f);
	private boolean showLPA      = false;
	private boolean animating    = true;
	private boolean debug        = false;
	private boolean fpsControl   = false;
	private boolean mousePressed = false;
	
	// TODO dirty hack
	private boolean pulse = false;
	private Vector4f pulseDir = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
	private Vector2f mouseMovement = new Vector2f(0.0f, 0.0f);

	/**
	 * Ctor.
	 */
	public MainProgram() {
	    
	}
	
	/**
	 * Initializes OpenGL, OpenCL, particles etc.
	 * @return true if everything works fine.
	 */
	public boolean init() {
		boolean success = true;
		
		if(success)
			success = initGL();
		
		if(success)
			success = initCL();
		
		if(success)
			success = initParticleBuffers();
		
		if(success)
			printControls();

		return success;
	}
	
	/**
	 * Initializes the particle buffers and constructs the OpenGL Buffer Objects.
	 * @return true
	 */
	private boolean initParticleBuffers() {
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
        
        // additional vertex array to be able to visualize the LPAs differently
        lpaVAID = glGenVertexArrays();
        glBindVertexArray(lpaVAID);
        
        FloatBuffer bufferLPA = ParticleFactory.createZeroFloatBuffer(numberLPA * 3);
        bufferObjectLPA = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferObjectLPA);
        glBufferData(GL_ARRAY_BUFFER, bufferLPA, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3 * SizeOf.FLOAT, 0);
        
        return true;
	}
	
	/**
	 * Initializes shared Buffers, sets Kernel args and holds the main Loop.
	 */
	public void run() {
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
        defaultSpawn = Math.min(defaultSpawn, elements);
        spawnElements = defaultSpawn;
        
        FloatBuffer bufferNewParticleData = BufferUtils.createFloatBuffer(spawnElements * ParticleFactory.PARTICLE_PROPERTIES);
        
        // create indices for LPA seeking
        IntBuffer bufferRandIndices = BufferUtils.createIntBuffer(elements);
       
        // print some information
        System.out.println("Running with " + elements + " Particles.");
        System.out.println("Respawning  ~" + (int)(0.7f*(elements>>1)) + " particles per second (Minimum " + defaultSpawn + " per frame).");
        System.out.println("Using " + numberLPA + " low pressure areas, changing position every ~" + changeLPAInterval + " ms.");

        // spawn first LPAs
        FloatBuffer bufferLPA = ParticleFactory.createLPA(numberLPA);
        glBindBuffer(GL_ARRAY_BUFFER, bufferObjectLPA);
        glBufferData(GL_ARRAY_BUFFER, bufferLPA, GL_STATIC_DRAW);
        memLPAs = CL10GL.clCreateFromGLBuffer(context, 0, bufferObjectLPA, null);
        
        // init pulse Dir buffer
        FloatBuffer pulseBuffer = ParticleFactory.createZeroFloatBuffer(5);
        CLMem memPulse = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_USE_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, pulseBuffer);
        
        while(running) {
			long deltaTime = System.currentTimeMillis() - lastTimestamp;
			lastTimestamp  += deltaTime;
			changeLPATimer += deltaTime;
			calculateFramesPerSecond(deltaTime);

			handleInput(deltaTime);
			
			if(animating) {
				// GENERATE NEW LPA, CHANGE WHICH LPA IS USED 
                if(changeLPATimer >= changeLPAInterval) {
                	changeLPATimer = 0;

                	bufferLPA = ParticleFactory.createLPA(numberLPA);
                    glBindBuffer(GL_ARRAY_BUFFER, bufferObjectLPA);
                    glBufferData(GL_ARRAY_BUFFER, bufferLPA, GL_STATIC_DRAW);
    	        	
                    for(int i = 0; i < elements; i++) {
                    	int id = (int)((numberLPA * numberLPA * ParticleFactory.lifetime() + numberLPA * ParticleFactory.lifetime() + numberLPA)) % 4;
                    	bufferRandIndices.put(i, id);
                    }
                    
                    if(memLPARandoms != null) {
                        OpenCL.clReleaseMemObject(memLPARandoms);
                        memLPARandoms = null;
                    }
                    memLPARandoms = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_ONLY, bufferRandIndices);
                }
                
                
                
				// ACQUIRE OPENGL BUFFERS
    			OpenCL.clEnqueueAcquireGLObjects(queue, memPositions,  null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memVelocities, null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memLifetimes,  null, null);
    			OpenCL.clEnqueueAcquireGLObjects(queue, memLPAs,       null, null);
    			
    			
    			
    			// MOVE PARTICLES
    			gws.put(0, elements);
    	        OpenCL.clSetKernelArg(kernelMove, 3, memLPAs);
    			OpenCL.clSetKernelArg(kernelMove, 4, memLPARandoms);
    			OpenCL.clSetKernelArg(kernelMove, 6, (int)deltaTime);
    			
    			// TODO dirty hack to test
    			OpenCL.clSetKernelArg(kernelMove, 7, pulse?1:0);
    			if(pulse) {
    				pulse = false;
    				pulseBuffer.put(0, pulseDir.x);
    				pulseBuffer.put(1, pulseDir.y);
    				pulseBuffer.put(2, pulseDir.z);
    				pulseBuffer.put(3, pulseDir.w);
    				
    				if(memPulse != null) {
    					OpenCL.clReleaseMemObject(memPulse);
    					memPulse = null;
    				}
    				memPulse = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_USE_HOST_PTR  | OpenCL.CL_MEM_READ_ONLY, pulseBuffer);
    			}
    			OpenCL.clSetKernelArg(kernelMove, 8, memPulse);
    			
    			OpenCL.clEnqueueNDRangeKernel(queue, kernelMove, 1, null, gws, null, null, null);
    			
    			
    			
    			// RESPAWN
    			int newCalc = defaultSpawn;
                newCalc = (int)fps>0? (int)(0.7f * ((elements>>1) / (int)fps)) : defaultSpawn;
                newCalc = Math.max(newCalc, defaultSpawn);
             
                // resize respawn buffer if needed
                if(newCalc != spawnElements) {
                    if(Math.abs(newCalc - spawnElements) > 0.1 * spawnElements) {
                        if(debug)
                            System.out.println("Resizing Respawn Buffer!");
                        spawnElements = newCalc;
                        
                        bufferNewParticleData = BufferUtils.createFloatBuffer(spawnElements * ParticleFactory.PARTICLE_PROPERTIES);
                    }
                }
                
                ParticleFactory.createNewParticles(bufferNewParticleData);
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
                
                
                
                // FREE OPENGL BUFFERS
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memLifetimes,  null, null);
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memVelocities, null, null);
    	        OpenCL.clEnqueueReleaseGLObjects(queue, memLPAs,       null, null);
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
	
	/**
	 * renders the scene
	 */
    public void drawScene() {
        Matrix4f viewProj = opengl.util.Util.mul(null, cam.getProjection(), cam.getView());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // post effects etc
        depthSP.use();
        depthSP.setUniform("model", modelMat);
        depthSP.setUniform("viewProj", viewProj);
        depthSP.setUniform("camPos", cam.getCamPos());
        depthSP.setUniform("noiseTex", noiseTex);

        depthFB.bind();
        depthFB.clearColor();
        
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        
        glBindVertexArray(vertexArrayID);
        opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, elements);
//      glowSP.use();
//      glowSP.setUniform("model", modelMat);
//      glowSP.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
//      glowSP.setUniform("camPos", cam.getCamPos());
//      glowSP.setUniform("depthTex", depthTex);
//      glowSP.setUniform("TexelSize", new Vector3f(1.0f/WIDTH, 1.0f/HEIGHT, 0.0f));
//      glowSP.setUniform("Sample0", depthTex);
//      glowSP.setUniform("Orientation", 0);
//      glowSP.setUniform("BlurAmount", 10);
//      glowSP.setUniform("BlurScale", 2.0f);
//      glowSP.setUniform("BlurStrength", 0.4f);
//
//      glowFB.bind();
//      glowFB.clearColor();
        
        
        blurSP.use();
        blurSP.setUniform("tex", depthTex);
        blurSP.setUniform("dir", 1);
        hBlurFB.bind();
        hBlurFB.clearColor();
        
//      glEnable(GL_BLEND);
//      glDisable(GL_DEPTH_TEST);
//      
//      glBindVertexArray(vertexArrayID);
//      opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, elements);
        
        screenQuad.draw();

//        glowSP.setUniform("Sample0", glowTex);
//        glowSP.setUniform("Orientation", 1);
        blurSP.setUniform("tex", hBlurTex);
        blurSP.setUniform("dir", 0);
        vBlurFB.bind();
        vBlurFB.clearColor();
        
        screenQuad.draw();
        
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        
        finalSP.use();
        finalSP.setUniform("depthTex", depthTex);
        finalSP.setUniform("blurTex", vBlurTex);
//      finalSP.setUniform("tex", hBlurTex);
//      finalSP.setUniform("dir", 0);
//      finalFB.bind();
//      finalFB.clearColor();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        screenQuad.draw();
        
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        
        
//      // draw texture on screenquad
//        glBindFramebuffer(GL_FRAMEBUFFER, 0);
//      screenQuadSP.use();        
//      screenQuadSP.setUniform("image", finalTex);
////        screenQuadSP.setUniform("image2", glowTex);
//      screenQuad.draw();
        
        // show low pressure areas with a blue tone

        if(showLPA) {
            lpaSP.use();
            lpaSP.setUniform("model", modelMat);
            lpaSP.setUniform("viewProj", viewProj);
            lpaSP.setUniform("camPos", cam.getCamPos());
            
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            
            glBindVertexArray(lpaVAID);
            opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, numberLPA);
        }

        // present screen
        Display.update();
        if(fpsControl)
            Display.sync(60);
    }
    
    /**
     * Handle keyboard and mouse inputs
     * @param deltaTime the time of the current frame.
     */
	private void handleInput(long deltaTime) {
        float speed = 1e-3f * deltaTime;
        float moveSpeed = 1e-3f * (float)deltaTime;

        // KEYBOARD HANDLING
        while(Keyboard.next()) {
            if(Keyboard.getEventKeyState()) {	// KEY DOWN
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W: moveDir.z += 1.0f; break;
                    case Keyboard.KEY_S: moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_A: moveDir.x += 1.0f; break;
                    case Keyboard.KEY_D: moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y += 1.0f; break;
                    case Keyboard.KEY_C: moveDir.y -= 1.0f; break;
                }
            } else { // KEY UP
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W:     moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_S:     moveDir.z += 1.0f; break;
                    case Keyboard.KEY_A:     moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_D:     moveDir.x += 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y -= 1.0f; break;
                    case Keyboard.KEY_C:     moveDir.y += 1.0f; break;
                    
                    case Keyboard.KEY_E: animating  = !animating; break;
                    case Keyboard.KEY_L: showLPA    = !showLPA;   break;
                    case Keyboard.KEY_H: debug      = !debug;
                                         System.out.println("Debug mode: " + (debug?"on":"off"));
                                             break;
                    case Keyboard.KEY_F: fpsControl = !fpsControl;
                                         if(debug) System.out.println("FPS " + (!fpsControl?"un":"") + "limited");
                                             break;
                                             
                    case Keyboard.KEY_R: ; break;
                }
            }
        }
        cam.move(moveSpeed * moveDir.z, moveSpeed * moveDir.x, moveSpeed * moveDir.y);

        
        // LMB PRESSED HANDLING
        if(mousePressed) {
        	mouseDelay += deltaTime;
        	if(mouseDelay >= mouseThreshold) {
        		if(debug) 
        			System.out.println("LMB released");
        		mouseDelay   = 0;
        		mousePressed = false;
        		mouseMovement.x -= Mouse.getX();
        		mouseMovement.y -= Mouse.getY();
        		if(debug)
        			System.out.println("Mouse difference: " + mouseMovement);
        		if(mouseMovement.length() != 0.0f) {
	        		pulse = true;
	        		pulseDir.set(mouseMovement.x, -mouseMovement.y);
	        		pulseDir.normalise();
	        		pulseDir.w = 10.0f * mouseMovement.lengthSquared()/(WIDTH*WIDTH + HEIGHT*HEIGHT);
	        		System.out.println(pulseDir);
        		}
        	}
        }
        
        // GENERAL MOUSE HANDLING
        while(Mouse.next()) {
            if(Mouse.isButtonDown(1)) {
                cam.rotate(-speed*Mouse.getEventDX(), -speed*Mouse.getEventDY());
            }
            if(Mouse.isButtonDown(0)) {
            	if(!mousePressed) {
            		if(debug) 
            			System.out.println("LMB press!");
            		mousePressed = true;
            		mouseMovement.set(Mouse.getX(), Mouse.getY());
            	}
            	mouseDelay = 0;
            }
        }
    }
	
	/**
	 * Initializes OpenGL, including the shader programs.
	 * @return true if initialization was successful.
	 */
	private boolean initGL() {
        try {
            GL.init();
        } catch (LWJGLException e) {
        	e.printStackTrace();
            return false;
        }
        cam.move(-1.0f, 0, 0.4f);
        
        // screenQuad
        screenQuad   = GeometryFactory.createScreenQuad();
        screenQuadSP = new ShaderProgram("shader/ScreenQuad_VS.glsl", "shader/CopyTexture_FS.glsl");
        
        // lpa debug visualization
        lpaSP = new ShaderProgram("shader/LPA_VS.glsl", "shader/LPA_FS.glsl");
        
        // first renderpath: "depth"
        depthSP = new ShaderProgram("./shader/DefaultVS.glsl", "./shader/Default1FS.glsl");
        depthSP.use();
        
        depthFB = new FrameBuffer();
        depthFB.init(true, WIDTH, HEIGHT);

        depthTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        depthFB.addTexture(depthTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(depthSP.getId(), 0, "PixelColor");
        
        // second renderpath: "glow"
        glowSP = new ShaderProgram("./shader/GlowVS.glsl", "./shader/GlowFS.glsl");
        glowSP.use();
        
        glowFB = new FrameBuffer();
        glowFB.init(true, WIDTH, HEIGHT);

        glowTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        glowFB.addTexture(glowTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(glowSP.getId(), 0, "PixelColor");
        
        // renderpath: "blur"
        blurSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/Blur_FS.glsl");
        blurSP.use();
        
        hBlurFB = new FrameBuffer();
        hBlurFB.init(true, WIDTH, HEIGHT);
        
        hBlurTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        hBlurFB.addTexture(hBlurTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(blurSP.getId(), 0, "PixelColor");
        
        vBlurFB = new FrameBuffer();
        vBlurFB.init(true, WIDTH, HEIGHT);
        
        vBlurTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        vBlurFB.addTexture(vBlurTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(blurSP.getId(), 0, "PixelColor");

        // renderPath: "blur"
        finalSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/FinalFS.glsl");
        finalSP.use();
        
        finalFB = new FrameBuffer();
        finalFB.init(true, WIDTH, HEIGHT);
        
        finalTex = new Texture(GL_TEXTURE_2D, textureUnit++);
        finalFB.addTexture(finalTex, GL_RGBA16F, GL_RGBA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFragDataLocation(finalSP.getId(), 0, "PixelColor");
        
        // noise texture
        noiseTex = Texture.generateTexture("./res/perlin.png", textureUnit++);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glBlendFunc(GL_ONE, GL_ONE);
        
        return true;
    }
	
	/**
	 * Initializes OpenCL, including the kernels.
	 * @return true if initialization was successful.
	 */
	private boolean initCL() {
        CLUtil.createCL();
        
        PlatformDevicePair pair = null;
        try {
            PlatformDeviceFilter filter = new PlatformDeviceFilter();
            
            // set spec here
            filter.addPlatformSpec(CL10.CL_PLATFORM_VENDOR, "NVIDIA");
            filter.setDesiredDeviceType(CL10.CL_DEVICE_TYPE_GPU);
                
            // query platform and device
            pair = CLUtil.choosePlatformAndDevice(filter);
        } catch(Exception e) {
            pair = CLUtil.choosePlatformAndDevice();
        }
        
        context  = OpenCL.clCreateContext(pair.platform, pair.device, null, Display.getDrawable());
        queue    = OpenCL.clCreateCommandQueue(context, pair.device, 0);
        // for out of order queue: OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        
        program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        kernelMove  = OpenCL.clCreateKernel(program, "move");
        kernelSpawn = OpenCL.clCreateKernel(program, "respawn");
        
        return true;
    }
	
	/**
	 * Breaks the main Loop, frees resources.
	 */
	private void stop() {
        running = false;
        
        // Shaderprograms
        if(screenQuadSP != null)
            screenQuadSP.delete();
        if(lpaSP != null)
            lpaSP.delete();
        if(depthSP != null)
            depthSP.delete();
        if(glowSP != null)
            glowSP.delete();
        
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
	 * Calculates FPS
	 * @param deltaTime current frame time
	 */
	private void calculateFramesPerSecond(long deltaTime) {
		numberOfFrames++;
		sumDeltaTime += deltaTime;
        if(sumDeltaTime > 1000) {
            fps = numberOfFrames / (float)(sumDeltaTime / 1000);
            numberOfFrames = 0;
            sumDeltaTime   = 0;
            Display.setTitle("FPS: " + fps);
        }
	}
	
	/**
	 * Prints the controls.
	 */
	private void printControls() {
	    String[] keyDesc = new String[]{
	        "W", "Move for",
	        "S", "Move back",
	        "A", "Move left",
	        "D", "Move right",
	        "SPACE", "Move up",
	        "C", "Move down",
	        "", "",
	        "F", "(Un-)Limit FPS to 60",
	        "L", "Show Low Pressure Areas",
	        "E", "Pause animation",
	        "H", "debug mode",
	        "", "",
	        "LMB", "Push flame (press and move to give direction) -WIP!",
	        "RMB", "Turn the camera",
	    };
	    System.out.println("\nControls:");
	    for(int i = 0; i < keyDesc.length; i+=2) {
	        System.out.printf("%-5s %s\n", keyDesc[i], keyDesc[i+1]);
	    }
	    System.out.println();
	}
}
