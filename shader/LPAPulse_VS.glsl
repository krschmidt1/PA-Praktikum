#version 330

uniform mat4 viewProj;
uniform vec3 camPos;
uniform vec3 color;

in vec3 positionMC;

out vec3 FragColor;

void main(void)
{
    gl_Position  = viewProj * vec4(positionMC, 1.0);
    gl_PointSize = 20.0f / distance(camPos, positionMC);
    FragColor    = color;
}