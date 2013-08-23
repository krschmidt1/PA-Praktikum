package particle;


import java.nio.FloatBuffer;
import java.util.Random;

import opengl.util.Util;

import org.lwjgl.BufferUtils;

import pa.util.math.MathUtil;

public class ParticleFactory {
    private static Random rng = new Random();
		
	public static float[] generateCoordinates() {
        float maxRadius = 0.35f;
        float radius = rng.nextFloat() * maxRadius - maxRadius / 2.0f;
        float phi    = rng.nextFloat() * Util.PI_MUL2;
        float theta  = rng.nextFloat() * Util.PI_DIV2;
        
	    return new float[]{
    	            radius * MathUtil.cos(theta) * MathUtil.cos(phi), 
                    radius * MathUtil.cos(theta) * MathUtil.sin(phi) * 0.1f - 0.5f, 
                    radius * MathUtil.sin(theta)
                };
	}
	
	public static float generateLifetime() {
	    return rng.nextFloat() * 14000 + 2000;
	}
	
	public static float[] generateVelocity() {
	    float x = rng.nextFloat() * 0.05f - 0.025f;
	    float y = rng.nextFloat() * 0.10f + 0.020f;
	    float z = rng.nextFloat() * 0.05f - 0.025f;
	    return new float[] {0,0,0};
//	    return new float[] {x, y, z};
	}
	
    public static FloatBuffer createZeroFloatBuffer(int capacity) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(capacity);
        for(int i = 0; i < fb.capacity(); i++) {
            fb.put(0);
        }
        fb.rewind();
        return fb;
    }
	
    private ParticleFactory() {}

    public static FloatBuffer createLPA(int n) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(n * 3);
		
		int[] levels = new int[] {
					(int)(0.3 * n),
					(int)(0.4 * n),
					(int)(0.2 * n),
					(int)(0.1 * n)
		};
		for(int level = 0; level < levels.length; level++) {
			for(int i = 0; i < levels[level]; i++) {
				float radius = 0.08f * levels[level];
				float a = rng.nextFloat();
				float b = rng.nextFloat();
				float x = (a * radius - radius / 2.0f) * MathUtil.cos(b * Util.PI_MUL2);
				float y = 0.3f * level + (rng.nextFloat() * 0.1f - 0.05f) - 0.3f;
				float z = (a * radius - radius / 2.0f) * MathUtil.sin(b * Util.PI_MUL2);
//				System.out.println(radius + ", " + x + ", " + y + ", " + z);
				fb.put(new float[]{x,y,z});
			}
		}
		fb.rewind();
		return fb;
	}
}
