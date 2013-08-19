package main;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
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
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

import particle.Particle;
import particle.ParticleFactory;

public class MainProgram {
	private boolean running = true;
	
	////// OPENCL BLOCK
	private CLContext context;
	private CLCommandQueue queue;
	private CLProgram program;
	private CLKernel kernel;
	private CLMem mem;

	////// OPENGL BLOCK
	private ShaderProgram shaderProgram  = null;
	private Matrix4f modelMat = new Matrix4f();
	private Matrix4f modelIT  = opengl.util.Util.transposeInverse(modelMat, null);
	private Camera   cam      = new Camera();
	
	////// Particles
	private Particle particle = null;
	private Particle[] particles;

	
	public MainProgram() {
		initGL();
		particle = new Particle(0.0f, 0.0f, 0.0f);
		ParticleFactory.addParticle(particle);
	}
	
	public void run() {

		initCL();
        
        int elements = 3;
        
//        float[] positions = new float[elements * 3];
//        
//        for(int i = 0; i < elements; i++) {
//        	positions[i*3] = (float)Math.random();
//        	positions[i*3+1] = (float)Math.random();
//        	positions[i*3+2] = (float)Math.random();
//        }
        
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(elements*3);
        
        particles = new Particle[elements];
        for(int i = 0; i < elements; i++) {
        	particles[i] = new Particle((float)Math.random(), (float)Math.random(), (float)Math.random());
        	positionBuffer.put(particles[i].getPositionAsFloats());
        }
        
//        positionBuffer.put(positions);
        positionBuffer.rewind();
        
        
        mem = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_WRITE, positionBuffer);
        
        OpenCL.clSetKernelArg(kernel, 0, mem);

//        int count = 0;
//		while(running && count < 10) {
		while(running) {
			// TODO
        
//			count++;
			
	        PointerBuffer gws = new PointerBuffer(elements);
	        gws.put(0, elements);

	        OpenCL.clEnqueueNDRangeKernel(queue, kernel, 1, null, gws, null, null, null);

	        OpenCL.clEnqueueReadBuffer(queue, mem, 0, 0, positionBuffer, null, null);

	        positionBuffer.rewind();
	        for(int i = 0; i < elements; i++) {
	        	particles[i].setPosition(positionBuffer.get(), positionBuffer.get(), positionBuffer.get());
	        }
	        positionBuffer.rewind();
	        
	        for(int i = 0; i < positionBuffer.capacity(); i++) {
	        	System.out.print((int)(positionBuffer.get(i)*100) + ", ");
	        	if(i%3==2) System.out.print("    ");
	        }
        	System.out.println();
		
			
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested()) {
				stop();
			}

		}
//		stop();
	}
	
	public void initCL() {

		CLUtil.createCL();
		
		PlatformDevicePair pair = CLUtil.choosePlatformAndDevice();
		
		context = OpenCL.clCreateContext(pair.platform, pair.device, null, null);
        
		queue = OpenCL.clCreateCommandQueue(context, pair.device, OpenCL.CL_QUEUE_PROFILING_ENABLE);
		// for out of order queue: OpenCL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE
        
        program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        kernel = OpenCL.clCreateKernel(program, "move");
	}
	
	public void stop() {
		running = false;
		
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
		System.out.println("test1.1");
		shaderProgram = new ShaderProgram("shaders/DefaultVS.glsl", "shaders/DefaultFS.glsl");
		
		glClearColor(0.0f, 0.3f, 0.0f, 1.0f);
	}
	
	public void drawScene() {
		// OPENGL BLOCK
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		shaderProgram.use();
		shaderProgram.setUniform("model", modelMat);
//		shaderProgram.setUniform("modelIT", modelIT);
		shaderProgram.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
		shaderProgram.setUniform("camPos", cam.getCamPos());

		particle.getPosition().x += 0.1f;
		// draws
		ParticleFactory.updateData();
        ParticleFactory.draw();
        
        // present screen
        Display.update();
        Display.sync(60);
	}
}
