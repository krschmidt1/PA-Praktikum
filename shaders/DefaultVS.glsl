#version 330

uniform CameraBlock
{
    mat4 View;
    mat4 Projection;
    mat4 ProjectionView;
    vec3 EyePos;
    float ViewDistance;
} g_Camera;

uniform ModelBlock
{
    mat4 Model;
    mat4 ModelInv;
} g_Model;

in vec3 VertexPos;
in vec3 VertexNormal;
in vec2 VertexTex;

out vec3 FragmentNormal;
out vec2 FragmentTex;
out vec4 FragmentPos;

void main(void)
{
    FragmentPos = g_Model.Model * vec4(VertexPos, 1.0);
    gl_Position = g_Camera.ProjectionView * FragmentPos;
    FragmentNormal = (vec4(VertexNormal, 0.0) * g_Model.ModelInv).xyz;
    FragmentTex = VertexTex;
}