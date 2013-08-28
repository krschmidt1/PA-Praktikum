#version 330

in vec4 FragmentPos;
in vec2 gl_PointCoord;

out vec4 PixelColor;

void main(void)
{
	PixelColor = vec4(0, 1, 1, 1);
}