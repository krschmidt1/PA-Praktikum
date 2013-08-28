package particle;


import java.nio.FloatBuffer;
import java.util.Random;

import opengl.util.Util;

import org.lwjgl.BufferUtils;

import pa.util.math.MathUtil;

public class ParticleFactory {
    public static final int PARTICLE_PROPERTIES = 7;

    private static Random rng = new Random();
    
    private static float spawnRadius   = 0.1f;
    private static float averageSpawnY = -0.5f;
    private static float spawnHeightY  = 0.1f;
    
    private static float defaultFlameRadius = 0.1f;
    
    private static long minLifetime = 10000;  // in ms
    private static long maxLifetime = 12000;  // in ms
    
    private static float minSpeed = 0.00f;
    private static float maxSpeed = 0.01f;
    
	public static float[] generateCoordinates() {
        float radius = rng.nextFloat() * spawnRadius - spawnRadius / 2.0f;
        float phi    = rng.nextFloat() * Util.PI_MUL2;
        float theta  = rng.nextFloat() * Util.PI_DIV2;
        
	    return new float[]{
    	            radius * MathUtil.cos(theta) * MathUtil.cos(phi), 
                    radius * MathUtil.cos(theta) * MathUtil.sin(phi) * spawnHeightY + averageSpawnY, 
                    radius * MathUtil.sin(theta)
                };
	}
	
	public static float lifetime() {
	    return rng.nextFloat() * (maxLifetime - minLifetime) + minLifetime;
	}
	
	public static float[] generateVelocity() {
	    float x = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * Math.signum(rng.nextFloat()-0.5f);
	    float y = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * 1.1f;
	    float z = (rng.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * Math.signum(rng.nextFloat()-0.5f);
	    float norm = MathUtil.sqrt(x*x+y*y+z*z);
	    return new float[] {x/norm, y/norm, z/norm};
//	    return new float[] {0,0,0};
	}
	
    public static FloatBuffer createZeroFloatBuffer(int capacity) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(capacity);
        for(int i = 0; i < fb.capacity(); i++) {
            fb.put(0);
        }
        fb.rewind();
        return fb;
    }
    
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
		        modifier[i] = modifier[i-1] + 0.2f * levels;
		    }
		    else if(i < levels * 0.9f) {
		        modifier[i] = modifier[i-1] - 0.1f * levels; 
		    }
		    else {
		        modifier[i] = modifier[0];
		    }
		}
		
		float y = averageSpawnY + spawnHeightY + 0.05f;
		float varianceY = 0.04f;
		
		for(int i = 0; i < levels; i++) {
			for(int j = 0; j < lpaPerLevel; j++) {
				
			    float phi = (float)j/(float)lpaPerLevel * MathUtil.PI_MUL2 + rng.nextFloat() * 0.01f - 0.005f;
				float radius = (float)rng.nextGaussian() * modifier[i] * defaultFlameRadius;
				
				// polar coordinates
				float x = radius * MathUtil.cos(phi);
				float z = radius * MathUtil.sin(phi);
				float curY = y + (float)rng.nextGaussian() * varianceY - varianceY / 2.0f;
				
				fb.put(x);
				fb.put(curY);
				fb.put(z);
			}
			y += (1.0f + Math.abs(averageSpawnY + spawnHeightY))/levels;
		}
		fb.rewind();
		return fb;
	}
    
    /**
     * creates 30 ordered LPA
     * @return
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

    private ParticleFactory() {}

	public static float generateRandomValue(float min, float max) {
		return min + rng.nextFloat() * (max - min);
	}

    /**
     * @param particleData
     */
    public static void createNewParticles(FloatBuffer particleData) {
        for(int i = 0; i < particleData.capacity(); i += PARTICLE_PROPERTIES) {
            int j = 0;
            float[] pos  = ParticleFactory.generateCoordinates();
            float[] velo = ParticleFactory.generateVelocity();
            particleData.put(i + j++, pos[0]);
            particleData.put(i + j++, pos[1]);
            particleData.put(i + j++, pos[2]);
            particleData.put(i + j++, velo[0]);
            particleData.put(i + j++, velo[1]);
            particleData.put(i + j++, velo[2]);
            particleData.put(i + j++, ParticleFactory.lifetime());
        }
        
    }
}
