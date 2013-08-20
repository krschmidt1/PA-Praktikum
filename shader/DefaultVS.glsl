#version 330

uniform mat4 model;
uniform mat4 modelIT;
uniform mat4 viewProj;
uniform vec3 camPos;

in vec3 positionMC;
in vec2 normalMC;

out vec4 FragmentPos;
out float alive;
void main(void)
{
	alive = normalMC.y;
    FragmentPos = model * vec4(positionMC, 1.0);
    gl_Position = viewProj * FragmentPos;
    float d = distance(camPos, positionMC);
    gl_PointSize = 20.0f/(1 + d * d);
}