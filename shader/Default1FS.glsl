#version 330

uniform sampler2D noiseTex; 

in vec4 FragmentPos;
in vec2 gl_PointCoord;
in vec2 lifetime;

out vec4 PixelColor;

void main(void)
{
	if(lifetime.y == 0.0f) discard;
  
  // make round particles
  float r = dot(gl_PointCoord * 2.0f - 1.0f, gl_PointCoord * 2.0f - 1.0f);
	if( r > 1.0f ) {
		PixelColor = vec4(0, 0, 0, 0);
		discard;
	}
		
	PixelColor = 0.8f * vec4(0.08f * (1.0f - r)*(1.0f - r), 0.03f * (1.0f - r)*(1.0f - r)*(1.0f - r), 0, r);
	
	//ivec2 noiseSize = textureSize(noiseTex, 0);
	vec2 noiseCoord = gl_PointCoord; 
	
	vec4 noise = texture(noiseTex, noiseCoord);
	
	if(FragmentPos.x > 0.0f) {
		PixelColor = noise * PixelColor;
	}else {
		PixelColor = 0.5f * PixelColor;
	}

	//if(alive == 0.0f) PixelColor = vec4(0.1*(1-r), 0.1*(1-r), 0.1*(1-r), r);
	//depth = vec4(1, 0, 0, r);
	
}