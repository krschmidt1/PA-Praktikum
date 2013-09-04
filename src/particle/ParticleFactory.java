package particle;


import java.nio.FloatBuffer;
import java.util.Random;

import opengl.util.Util;

import org.lwjgl.BufferUtils;

import pa.util.math.MathUtil;

public class ParticleFactory {
    public static final int PARTICLE_PROPERTIES = 7;

    private static Random rng = new Random();
    
    private static float spawnRadius   = 0.3f;
    private static float averageSpawnY = -0.5f;
    private static float spawnHeightY  = 0.1f;
    
    private static float defaultFlameRadius = 0.1f;
    
    private static long minLifetime = 1500;//2100;  // in ms
    private static long maxLifetime = 1700;//2400;  // in ms
    
    private static float minSpeed = 0.00f;
    private static float maxSpeed = 0.01f;
    
    /**
     * Generates x, y and z coordinates in the sphere around (0, 0, 0) with radius spawnRadius.
     * @return x, y, z coordinates 
     */
	public static float[] generateCoordinates() {
        float radius = MathUtil.pow(rng.nextFloat(), 1.0f / 3.0f) * spawnRadius - spawnRadius / 2.0f;
        float phi    = rng.nextFloat() * Util.PI_MUL2;
        float theta  = rng.nextFloat() * Util.PI_DIV2;
        
	    return new float[]{
    	            radius * MathUtil.cos(theta) * MathUtil.cos(phi), 
                    radius * MathUtil.cos(theta) * MathUtil.sin(phi) * spawnHeightY + averageSpawnY, 
                    radius * MathUtil.sin(theta)
                };
	}
	
	/**
	 * Generates a lifetime between min and maxLifetime.
	 * @return a random lifetime
	 */
	public static float lifetime() {
	    return rng.nextFloat() * (maxLifetime - minLifetime) + minLifetime;
	}
	
	/**
	 * Generates a velocity direction.
	 * @return normalized velocity direction.
	 */
	public static float[] generateVelocity() {
	    float x = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * Math.signum(rng.nextFloat()-0.5f);
	    float y = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * 1.1f;
	    float z = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * Math.signum(rng.nextFloat()-0.5f);
	    float norm = MathUtil.sqrt(x*x+y*y+z*z);
	    return new float[] {x/norm, y/norm, z/norm};
//	    return new float[] {0,0,0};
	}
	
	/**
	 * Creates a Buffer and fills it with 0.
	 * @param capacity number of elements
	 * @return FloatBuffer filled with 0.
	 */
    public static FloatBuffer createZeroFloatBuffer(int capacity) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(capacity);
        for(int i = 0; i < fb.capacity(); i++) {
            fb.put(0);
        }
        fb.rewind();
        return fb;
    }
    
    /**
     * Spawns n LPA to give the flame some shape.
     * @param n number of LPAs to spawn
     * @return FloatBuffer with n*3 elements, which are positional x, y, z coords.
     */
    public static FloatBuffer createLPA(int n) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(n * 3);
		
		int lpaPerLevel = 8;
		int levels = n/lpaPerLevel;
		
		// Radius modifier 
		// TODO needs improvement?
		float[] modifier = new float[levels];
		modifier[0] = 1.0f;
		for(int i = 1; i < modifier.length; i++) {
		    if(i < levels * 0.3f) {
		        modifier[i] = modifier[i-1] + 0.1f * levels;
		    }
		    else if(i < levels * 0.9f) {
		        modifier[i] = modifier[i-1] - 0.1f * levels; 
		    }
		    else if(i == levels-1) {
		    	modifier[i] = 2.5f;
		    }
		    else {
		        modifier[i] = modifier[0];
		    }
		}
		
		float y = averageSpawnY + spawnHeightY + 0.05f;
		float varianceY = 0.04f;
		
		for(int i = 0; i < levels; i++) {
			for(int j = 0; j < lpaPerLevel; j++) {
				
			    float phi = (float)j/(float)lpaPerLevel * MathUtil.PI_MUL2 + rng.nextFloat() * 0.4f - 0.2f;
				float radius = (float)rng.nextGaussian() * modifier[i] * defaultFlameRadius;
				
				// polar coordinates
				float x = radius * MathUtil.cos(phi);
				float z = radius * MathUtil.sin(phi);
				float curY = y + (float)rng.nextGaussian() * varianceY - varianceY / 2.0f;
				
				fb.put(x);
				fb.put(curY);
				fb.put(z);
			}
			y += 1.7f * (1.0f + Math.abs(averageSpawnY + spawnHeightY))/levels;
		}
		fb.rewind();
		return fb;
	}
    
    /**
     * Creates 30 ordered LPA, only useful for debug purposes.
     * @return A buffer containing the LPA positions.
     */
    public static FloatBuffer createOrderedLPA() {
		FloatBuffer fb = BufferUtils.createFloatBuffer(30 * 3);

		float[] r = new float[]{0.15f, 0.25f, 0.22f, 0.16f, 0.1f};
		float[] phi = new float[6];
		for(int i = 0; i < 6; i++) 
		{
			phi[i] = (float)i/6.0f * MathUtil.PI_MUL2;
		}
		float y = -0.2f;
		for(int i = 0; i < 5; i++) {
			for(int j = 0; j < 6; j++) {
				float x = r[i] * MathUtil.cos(phi[j]);
				float z = r[i] * MathUtil.sin(phi[j]);
				fb.put(x);
				fb.put(y);
				fb.put(z);
			}
			y+=0.2f;
		}
		fb.rewind();
		return fb;
	}

    /**
     * private Ctor to avoid instantiation.
     */
    private ParticleFactory() {}

    /**
     * Generates a random value between min and max.
     * @param min min value
     * @param max max value
     * @return value from half open intveral [min,...,max[
     */
	public static float generateRandomValue(float min, float max) {
		return min + rng.nextFloat() * (max - min);
	}

    /**
     * Fills new particles inside the particleData buffer.
     * @param particleData the particle buffer
     */
    public static void createNewParticles(FloatBuffer particleData) {
    	particleData.rewind();
        for(int i = 0; i < particleData.capacity(); i += PARTICLE_PROPERTIES) {
            particleData.put(ParticleFactory.generateCoordinates());
            particleData.put(ParticleFactory.generateVelocity());
            particleData.put(ParticleFactory.lifetime());
        }
        particleData.rewind();
        
    }
}
