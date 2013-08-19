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
	
	////// SHARED BLOCK
	private int bufferObject = -1;
	private int elements = 3; 

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
	
	private int vertexArrayID = -1;
	
	public MainProgram() {
		initGL();
		initCL();

		initParticles();
	}
	
	private void initParticles() {
		bufferObject = glGenBuffers();
		
		glBindBuffer(GL_ARRAY_BUFFER, bufferObject);

		// generate particles
		for(int i = 0; i < elements; i++) {
			ParticleFactory.createParticle();
		}
		FloatBuffer particleData = ParticleFactory.getParticleData();
		
		glBufferData(GL_ARRAY_BUFFER, particleData, GL_STATIC_DRAW);
		
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3*4, 0);
		
	}

	public void run() {
        
//        mem = OpenCL.clCreateBuffer(context, OpenCL.CL_MEM_COPY_HOST_PTR | OpenCL.CL_MEM_READ_WRITE, positionBuffer);
        mem = OpenCL.clCreateFromGLBuffer(context, OpenCL.CL_MEM_READ_WRITE, bufferObject);
        OpenCL.clSetKernelArg(kernel, 0, mem);
        
        PointerBuffer gws = new PointerBuffer(elements);
        gws.put(0, elements);

		while(running) {
	        
	        
			OpenCL.clEnqueueAcquireGLObjects(queue, mem, null, null);
	        OpenCL.clEnqueueNDRangeKernel(queue, kernel, 1, null, gws, null, null, null);
	        OpenCL.clEnqueueReleaseGLObjects(queue, mem, null, null);
	        

	        
//	        OpenCL.clEnqueueReadBuffer(queue, mem, 0, 0, positionBuffer, null, null);
//
//	        positionBuffer.rewind();
//	        for(int i = 0; i < elements; i++) {
//	        	particles[i].setPosition(positionBuffer.get(), positionBuffer.get(), positionBuffer.get());
//	        }
//	        positionBuffer.rewind();
//	        
//	        for(int i = 0; i < positionBuffer.capacity(); i++) {
//	        	System.out.print((int)(positionBuffer.get(i)*100) + ", ");
//	        	if(i%3==2) System.out.print("    ");
//	        }
//        	System.out.println();
		
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested()) {
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
		shaderProgram = new ShaderProgram("shaders/DefaultVS.glsl", "shaders/DefaultFS.glsl");
		
		glClearColor(0.1f, 0.0f, 0.4f, 1.0f);
		
		vertexArrayID = glGenVertexArrays();
		glBindVertexArray(vertexArrayID);
	}
	
	public void drawScene() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		shaderProgram.use();
		shaderProgram.setUniform("model", modelMat);
//		shaderProgram.setUniform("modelIT", modelIT);
		shaderProgram.setUniform("viewProj", opengl.util.Util.mul(null, cam.getProjection(), cam.getView()));
		shaderProgram.setUniform("camPos", cam.getCamPos());

        ParticleFactory.draw();
        
        // present screen
        Display.update();
        Display.sync(60);
	}
}
