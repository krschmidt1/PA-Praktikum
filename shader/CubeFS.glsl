#version 330

uniform sampler2D tex;
uniform sampler2D normalTex;

in vec4 FragmentPos;
in vec4 normalWC;
in vec2 uv;
in vec3[12] lights;

out vec4 PixelColor;

const int number = 3;


void main(void) {

	// read normal from normal texture
	vec4 normal = normalize(vec4(texture(normalTex, uv).xyz - 0.5f, 0.0f));
	
	
	// phong-like diffuse light
	vec4 diffColor = texture(tex, uv);
	vec4 specColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);

	float diffScalar = 0.0f;

	for(int i = 0; i < number; i++) {
	
		vec4 light = vec4(lights[i], 1.0f);

		vec4 invLightDir = normalize(light - FragmentPos);
		float dist = length(FragmentPos - light);

		diffScalar += dot(normal, invLightDir) * dot(normal, invLightDir);
	}
	diffScalar /= number;	


	PixelColor = diffScalar * diffColor + 0.2 * diffScalar * vec4(1.0f, 0.4f, 0.0f, 1.0f);


}


