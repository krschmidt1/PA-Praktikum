#version 330

uniform sampler2D text;

in vec4 FragmentPos;
in vec4 normalWC;
in vec2 uv;

out vec4 PixelColor;


void main(void) {

	PixelColor = texture(text, uv);//vec4(1,0,0,1);//vec4(uv,0,1);//

}