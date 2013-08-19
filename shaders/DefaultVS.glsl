#version 330

uniform mat4 model;
uniform mat4 modelIT;
uniform mat4 viewProj;
uniform mat4 camPos;

in vec3 VertexPos;

out vec4 FragmentPos;

void main(void)
{
    FragmentPos = model * vec4(VertexPos, 1.0);
    gl_Position = viewProj * FragmentPos;
    gl_PointSize = 20.0f;
}