#version 330

in vec2 texCoord;

uniform sampler2D depthTex;
uniform sampler2D blurTex;
uniform sampler2D bgTex;
uniform sampler2D rgNoiseTex;

out vec4 PixelColor;

void main(void) {

	vec4 fireColor = texture(depthTex, texCoord);
	
	vec2 noise = (texture(rgNoiseTex, texCoord).xy-0.5f)/300;
	vec2 bgTexCoord = texCoord + noise*fireColor.w;
	
	PixelColor = fireColor + texture(blurTex, texCoord);
	PixelColor = mix(PixelColor, texture(bgTex, bgTexCoord), clamp(1-(PixelColor.x+PixelColor.y)/2, 0, 1));

}
