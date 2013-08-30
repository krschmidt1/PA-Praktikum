#version 330

uniform sampler2D depthTex; 

uniform vec3 TexelSize;
uniform sampler2D Sample0;
uniform int Orientation;
uniform int BlurAmount;
uniform float BlurScale;
uniform float BlurStrength;

in vec4 FragmentPos;
in vec2 gl_PointCoord;
in vec2 lifetime;
in vec2 texCoords;

out vec4 PixelColor;

float Gaussian (float x, float deviation) {
	return (1.0 / sqrt(2.0 * 3.141592 * deviation)) * exp(-((x * x) / (2.0 * deviation)));
}


void main(void)
{
	if(lifetime.y == 0.0f) discard;
  
  // make round particles
  float r = dot(gl_PointCoord * 2.0f - 1.0f, gl_PointCoord * 2.0f - 1.0f);
	if( r > 1.0f ) {
		PixelColor = vec4(0, 0, 0, 0);
		discard;
	}
		
	PixelColor = vec4(0.0008f * (1.0f - r)*(1.0f - r), 0.0003f * (1.0f - r)*(1.0f - r)*(1.0f - r), 0, r);
	
}

void function(void)
{
	// Locals
	float halfBlur = float(BlurAmount) * 0.5;
	vec4 colour = vec4(0.0);
	vec4 texColour = vec4(0.0);
	// Gaussian deviation
	float deviation = halfBlur * 0.35;
	deviation *= deviation;
	float strength = 1.0 - BlurStrength;
	
	if ( Orientation == 0 ) {
		// Horizontal blur
		for (int i = 0; i < 10; ++i) {
			if ( i >= BlurAmount ) break;
			float offset = float(i) - halfBlur;
			texColour = texture2D(Sample0, texCoords + vec2(offset * TexelSize.x * BlurScale, 0.0)) * Gaussian(offset * strength, deviation);
			colour += texColour;
		}
	} else {
		// Vertical blur
		for (int i = 0; i < 10; ++i) {
			if ( i >= BlurAmount ) break;
			float offset = float(i) - halfBlur;
			texColour = texture2D(Sample0, texCoords + vec2(0.0, offset * TexelSize.y * BlurScale)) * Gaussian(offset * strength, deviation);
			colour += texColour;
		}
	}
	
	// Apply color
	 PixelColor = clamp(colour, 0.0, 1.0);
	 PixelColor.w = 1.0;
}

