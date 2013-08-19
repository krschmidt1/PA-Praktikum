package particle;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import opengl.util.ShaderProgram;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import static opengl.GL.*;

public class Particle {
	private Vector3f position;
	private int vaid = -1;
	
	public Particle() {
		
	}
	
	public Particle(float x, float y, float z) {
		position = new Vector3f(x, y, z);
	}
	
	public void draw() {
	    if(vaid == -1) {
	        createVA();
	    }
	    glBindVertexArray(vaid);
	    glDrawArrays(GL_POINTS, 0, 1);
	}
	
	/**
	 * creates a vertex array
	 */
	private void createVA() {
		FloatBuffer vertexData = BufferUtils.createFloatBuffer(3);
        vertexData.put(new float[]{position.x, position.y, position.z});
        vertexData.flip();
        
        vaid = glGenVertexArrays();
        glBindVertexArray(vaid);
        
        int vertexBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);
        glVertexAttribPointer(ShaderProgram.ATTR_POS, 3, GL_FLOAT, false, 3*4, 0);
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public float[] getPositionAsFloats() {
		return new float[]{position.x, position.y, position.z};
	}
	
	public String toString() {
		return "Particle: \n"
		     + "\tPosition: (" + position.x + ", " + position.y + ", " + position.z + ")\n"
		     ;
	}
}
