#version 330

in vec2 texCoord;

uniform sampler2D depthTex;
uniform sampler2D blurTex;
uniform sampler2D bgTex;

out vec4 PixelColor;

void main(void) {

	PixelColor = texture(depthTex, texCoord) + texture(blurTex, texCoord);
	PixelColor = mix(PixelColor, texture(bgTex, texCoord), clamp(1-(PixelColor.x+PixelColor.y)/2, 0, 1));

}
