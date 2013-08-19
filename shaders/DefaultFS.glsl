#version 330

uniform sampler2D g_AmbientTex;
uniform sampler2D g_DiffuseTex;
uniform sampler2D g_SpecularTex;
uniform sampler2D g_AlphaTex;

uniform MaterialBlock
{
    vec4 AmbientColor;
    vec4 DiffuseColor;
    vec4 SpecularColor;
    vec4 UseTextures;
} g_Material;

in vec3 FragmentNormal;
in vec2 FragmentTex;

out vec4 PixelColor;

void main(void)
{
    vec3 normal = normalize(FragmentNormal);
    vec4 diffuse = g_Material.DiffuseColor;
    if(g_Material.UseTextures.w > 0.5)
    {
        float alpha = texture(g_AlphaTex, FragmentTex * vec2(1, -1)).r;
        if(alpha > 0.95)
        {
            discard;
        }
    }
    if(g_Material.UseTextures.y > 0.5)
    {
        diffuse = texture(g_DiffuseTex, FragmentTex * vec2(1, -1));
    }
    PixelColor = 0 * g_Material.AmbientColor + vec4(diffuse.rgb * (0.6 + 0.5 * normal.y), diffuse.a);
    // if(diffuse.a == 0.0) {
        // discard;
    // }
    //PixelColor = vec4(FragmentNormal,1);
}