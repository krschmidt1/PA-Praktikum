#version 330

in vec3 FragColor;

out vec4 PixelColor;

void main(void)
{
    PixelColor = vec4(FragColor, 1);
}