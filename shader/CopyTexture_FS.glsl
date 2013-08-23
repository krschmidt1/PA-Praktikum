#version 330

uniform sampler2D image;

in vec2 texCoord;

out vec4 fragColor;

void main(void)
{
    fragColor = texture(image, texCoord);
    //vec4 bgColor = vec4(0.1f, 0.0f, 0.3f, 1.0f);
    //fragColor = mix(fragColor, bgColor, fragColor.w);
}