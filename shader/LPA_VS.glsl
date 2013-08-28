#version 330

uniform mat4 model;
uniform mat4 viewProj;
uniform vec3 camPos;

in vec3 positionMC;

out vec4 FragmentPos;

void main(void)
{
    FragmentPos  = model * vec4(positionMC, 1.0);
    gl_Position  = viewProj * FragmentPos;
    float d      = distance(camPos, positionMC);
    gl_PointSize = 20.0f / (1 + d * d);
}