package particle;


import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;


import org.lwjgl.BufferUtils;

public class ParticleFactory {
	
	private static ArrayList<Particle> particles = new ArrayList<Particle>();
	
	private ParticleFactory() {
	}

	public static void draw() {
	    opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, particles.size());
	}
		
	public static void createParticle() {
		Random r = new Random();
		particles.add(new Particle(r.nextFloat(), -r.nextFloat(), r.nextFloat()));
	}
	
	public static void addParticle(Particle particle) {
		particles.add(particle);
	}

	public static FloatBuffer getParticleData() {
		FloatBuffer fb = BufferUtils.createFloatBuffer(particles.size() * 3);
		for(Particle particle : particles) {
			fb.put(particle.getPositionAsFloats());
		}
		fb.rewind();
		return fb;
	}
}
