package particle;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

public class Particle {
	private Vector3f position;
	private float alive = 0.0f;
	private float lifetime = 0;
	
	public Particle() {
		
	}
	
	public Particle(float x, float y, float z, float lifetime, boolean alive) {
		position = new Vector3f(x, y, z);
		this.lifetime = lifetime;
		if(alive)
			this.alive = 1.0f;
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public void setPosition(float x, float y, float z) {
		position = new Vector3f(x, y, z);
	}
	
	public float[] getPositionAsFloats() {
		return new float[]{position.x, position.y, position.z};
	}
	
	public float getLifetime() {
		return lifetime;
	}
	
	public String toString() {
		return "Particle: \n"
		     + "\tPosition: (" + position.x + ", " + position.y + ", " + position.z + ")\n"
		     + "\tLifetime: " + lifetime + "\n"
		     + "\talive:    " + alive + "\n"
		     ;
	}

	public float getAlive() {
		return alive;
	}
	
//	public static int getNumberOfFloatValues() {
//		return 5;
//	}
}
