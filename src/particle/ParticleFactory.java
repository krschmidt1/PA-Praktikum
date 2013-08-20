package particle;


import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;


import opengl.util.Util;

import org.lwjgl.BufferUtils;

import pa.util.math.MathUtil;

public class ParticleFactory {
	
	private static ArrayList<Particle> particles = new ArrayList<Particle>();
	
	private ParticleFactory() {
	}

	public static void draw() {
	    opengl.GL.glDrawArrays(opengl.GL.GL_POINTS, 0, particles.size());
	}
		
	public static void createParticle() {
		Random r = new Random();
		float maxRadius = 0.35f;
		float radius = r.nextFloat() * maxRadius - maxRadius / 2.0f;
		float phi    = r.nextFloat() * Util.PI_MUL2;
		float theta  = r.nextFloat() * Util.PI_DIV2;

		float lifetime = r.nextFloat() * 10000;
		boolean alive = true;
		if(lifetime < 2500)
			alive = false;
		
		particles.add(new Particle(radius * MathUtil.cos(theta) * MathUtil.cos(phi), // x 
								   radius * MathUtil.cos(theta) * MathUtil.sin(phi), // y
								   radius * MathUtil.sin(theta),				     // z
								   lifetime,
								   alive));
	}
	
	public static void addParticle(Particle particle) {
		particles.add(particle);
	}

//	public static FloatBuffer getParticleData() {
//		FloatBuffer fb = BufferUtils.createFloatBuffer(particles.size() * Particle.getNumberOfFloatValues());
//		for(Particle particle : particles) {
//			fb.put(particle.getPositionAsFloats());
//		}
//		fb.rewind();
//		return fb;
//	}

	public static FloatBuffer getParticlePositions() {
		FloatBuffer fb = BufferUtils.createFloatBuffer(particles.size() * 3);
		for(Particle particle : particles) {
			fb.put(particle.getPositionAsFloats());
		}
		fb.rewind();
		return fb;
	}

	public static FloatBuffer getParticleLifetime() {
		FloatBuffer fb = BufferUtils.createFloatBuffer(particles.size() * 2);
		for(Particle particle : particles) {
			fb.put(particle.getLifetime());
			fb.put(particle.getAlive());
		}
		fb.rewind();
		return fb;
	}
}
