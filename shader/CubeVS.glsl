#version 330

uniform mat4 model;
uniform mat4 modelIT;
uniform mat4 viewProj;
uniform vec3 camPos;
uniform vec3[64] lightPos;

in vec3 positionMC;
in vec3 normalMC;
in vec2 vertexTexCoords;

out vec4 FragmentPos;
out vec4 normalWC;
out vec2 uv;
out vec3[64] lights;

void main(void)
{
    normalWC     = model * vec4(normalMC, 0.0);
    FragmentPos  = model * vec4(positionMC, 1.0);
    gl_Position  = viewProj * FragmentPos;
	uv = vertexTexCoords;
	lights=lightPos;
}
