package main;

import opengl.GL;
import static opengl.GL.*;
import opengl.util.Camera;
import opengl.util.ShaderProgram;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

import particle.Particle;

public class MainProgram {
	private boolean running = true;
	
	////// OPENGL BLOCK
	private ShaderProgram shaderProgram  = null;
	private Matrix4f modelMat = new Matrix4f();
	private Matrix4f modelIT  = opengl.util.Util.transposeInverse(modelMat, null);
	private Camera   cam      = new Camera();
	
	////// Particles
	private Particle particle = null;
	
	public MainProgram() {
		initGL();
		particle = new Particle(0.0f, 0.0f, 0.0f);
	}
	
	public void run() {
		
		
		while(running) {
			
			drawScene();
            
            // if close is requested: close
			if(Display.isCloseRequested()) {
				stop();
			}
		}
	}
	
	public void stop() {
		running = false;
		GL.destroy();
	}
	
	public void initGL() {
		try {
			GL.init();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
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
        
		// draws
        particle.draw();
        
        // present screen
        Display.update();
        Display.sync(60);
	}
}
