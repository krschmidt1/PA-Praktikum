#version 330

in vec4 FragmentPos;
in vec2 gl_PointCoord;
in vec2 lifetime;

out vec4 PixelColor;
out vec4 depth;

void main(void)
{
	if(lifetime.y == 0.0f) discard;
  
  // make round particles
  float r = dot(gl_PointCoord * 2.0f - 1.0f, gl_PointCoord * 2.0f - 1.0f);
	if( r > 1.0f ) {
		depth = vec4(0, 0, 0, 0);
		discard;
	}
		
	PixelColor = vec4(0.8f * (1.0f - r), 0.1f * (1.0f - r), 0, r);
	//if(alive == 0.0f) PixelColor = vec4(0.1*(1-r), 0.1*(1-r), 0.1*(1-r), r);
	//depth = vec4(1, 0, 0, r);
}