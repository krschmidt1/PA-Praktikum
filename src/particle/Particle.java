package particle;

import org.lwjgl.util.vector.Vector3f;

public class Particle {
	private Vector3f position;
	private int vaid = -1;
	
	public Particle() {
		
	}
	
	public Particle(float x, float y, float z) {
		position = new Vector3f(x, y, z);
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
	
	public String toString() {
		return "Particle: \n"
		     + "\tPosition: (" + position.x + ", " + position.y + ", " + position.z + ")\n"
		     ;
	}
}
