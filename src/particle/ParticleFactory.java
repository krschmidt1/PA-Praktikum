package particle;


import java.nio.FloatBuffer;
import java.util.Random;

import opengl.util.Util;

import org.lwjgl.BufferUtils;

import pa.util.math.MathUtil;

public class ParticleFactory {
    private static Random rng = new Random();
    private static int numberOfProperties = 3 + 3 + 2;
		
	public static float[] generateCoordinates() {
        float maxRadius = 1.0f;
        float radius = rng.nextFloat() * maxRadius - maxRadius / 2.0f;
        float phi    = rng.nextFloat() * Util.PI_MUL2;
        float theta  = rng.nextFloat() * Util.PI_DIV2;
        
	    return new float[]{
    	            radius * MathUtil.cos(theta) * MathUtil.cos(phi), 
                    radius * MathUtil.cos(theta) * MathUtil.sin(phi) * 0.1f - 0.5f, 
                    radius * MathUtil.sin(theta)
                };
	}
	
	public static float lifetime() {
	    return rng.nextFloat() * 2000.0f + 8000.0f;
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

		float[] r = new float[]{0.3f, 0.5f, 0.35f, 0.2f, 0.1f};
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
//				System.out.println(x + ", " + y + ", " + z);
				fb.put(x);
				fb.put(y);
				fb.put(z);
			}
			y+=0.2f;
		}
		fb.rewind();
		return fb;
	}
}
