#version 330

in vec2 texCoord;

uniform sampler2D depthTex;
uniform sampler2D blurTex;

out vec4 PixelColor;

void main(void) {

	PixelColor = texture(depthTex, texCoord) + texture(blurTex, texCoord);

}
