#version 330

in vec4 FragmentPos;
in vec2 gl_PointCoord;

out vec4 PixelColor;

void main(void)
{
	// make round particles
	if( dot(gl_PointCoord * 2.0f - 1.0f, 
			gl_PointCoord * 2.0f - 1.0f) >1.0f )
		discard;
		
		
	PixelColor = vec4(1, 0, 0, 1);
}