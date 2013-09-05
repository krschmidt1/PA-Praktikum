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
	
	//if(texCoord.x > 0.5f) {
		PixelColor = fireColor;
		PixelColor = mix(PixelColor, texture(bgTex, bgTexCoord), clamp(1-(PixelColor.x+PixelColor.x)/4, 0, 1));
		
		vec4 blur = texture(blurTex, texCoord);
		//blur.y = blur.x;
		PixelColor = PixelColor + blur;
	//}else {
	//	PixelColor = fireColor;
		//PixelColor = mix(PixelColor, texture(bgTex, bgTexCoord), clamp(1-(PixelColor.x+PixelColor.x)/5, 0, 1));
		//PixelColor = mix(PixelColor, texture(bgTex, texCoord), clamp(1-PixelColor.x, 0, 1));
	//}
	//PixelColor = mix(PixelColor, texture(bgTex, bgTexCoord), clamp(1-(PixelColor.x+PixelColor.x)/5, 0, 1));

}
