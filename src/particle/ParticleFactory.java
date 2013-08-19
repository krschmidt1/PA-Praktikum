package particle;

import static opengl.GL.GL_ARRAY_BUFFER;
import static opengl.GL.GL_FLOAT;
import static opengl.GL.GL_POINTS;
import static opengl.GL.GL_STATIC_DRAW;
import static opengl.GL.glBindBuffer;
import static opengl.GL.glBindVertexArray;
import static opengl.GL.glBufferData;
import static opengl.GL.glDrawArrays;
import static opengl.GL.glEnableVertexAttribArray;
import static opengl.GL.glGenBuffers;
import static opengl.GL.glGenVertexArrays;
import static opengl.GL.glVertexAttribPointer;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import opengl.util.ShaderProgram;

import org.lwjgl.BufferUtils;

public class ParticleFactory {
	private static int vaid = -1;
	
	private static ArrayList<Particle> particles = new ArrayList<Particle>();
	
	private static FloatBuffer vertexData = null;
	
	private ParticleFactory() {
	}

	public static void draw() {
	    if(vaid == -1) {
	    	vaid = glGenVertexArrays();
	        glBindVertexArray(vaid);
	        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
	        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3*4, 0);
	    }
	    glBindVertexArray(vaid);
	    glDrawArrays(GL_POINTS, 0, particles.size());
	}
		
	public static void updateData() {
		vertexData = BufferUtils.createFloatBuffer(3);
		for(Particle particle : particles) {
			vertexData.put(particle.getPositionAsFloats());
			System.out.println(particle);
		}
        vertexData.flip();
        
        int vertexBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
	}
	
	public static void createParticle() {
		
	}
	
	public static void addParticle(Particle particle) {
		particles.add(particle);
	}
}
