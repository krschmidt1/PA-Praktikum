#version 330

uniform mat4 model;
uniform mat4 modelIT;
uniform mat4 viewProj;
uniform vec3 camPos;

in vec3 positionMC;
in vec2 normalMC;

out vec4 FragmentPos;
out vec2 lifetime;
out vec2 texCoords;

void main(void)
{
    lifetime     = normalMC;
    FragmentPos  = model * vec4(positionMC, 1.0);
    gl_Position  = viewProj * FragmentPos;//vec4(positionMC, 0.0, 1.0);//viewProj * FragmentPos;
    float d      = distance(camPos, positionMC);
    gl_PointSize = 80.0f / (1 + d * d) * normalMC.x / 8000.0f;
    
    //texCoords = vec2(0.5) + 0.5 * positionMC;
}