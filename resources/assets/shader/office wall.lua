return {
	vertex = [[
		#version 460 core

		layout(location=0) in vec3 aLocation;
		layout(location=1) in vec2 aTexture;
		layout(location=2) in vec3 aNormal;
		layout(location=3) in vec4 aColor;

		layout(std140, binding=0) uniform MATRICES {
			mat4 matrixProjection;
			mat4 matrixView;
			mat4 matrixWorld;
		};

		layout(location=0) out struct {
			vec2 texture;
			vec4 color;
			vec3 normal;
		} v_v;

		void main ()
		{
			mat4 matrixWVP = matrixProjection * matrixView * matrixWorld;
			gl_Position = matrixWVP * vec4(aLocation, 1.0);
			v_v.texture = aTexture;
			v_v.color = aColor;
			v_v.normal = normalize((matrixWorld * vec4(aNormal, 0.0)).xyz);
		}
	]],
	fragment = [[
		#version 460 core

		float blendOverlay (float base, float blend)
		{
			return base < 0.5 ?
				(2.0 * base * blend) :
				(1.0 - 2.0 * (1.0 - base) * (1.0 - blend));
		}
		vec3 blendOverlay (vec3 base, vec3 blend)
		{
			return vec3(
				blendOverlay(base.x, blend.x),
				blendOverlay(base.y, blend.y),
				blendOverlay(base.z, blend.z)
			);
		}

		layout(location=0) in struct {
			vec2 texture;
			vec4 color;
			vec3 normal;
		} v_v;


		layout(binding=0) uniform sampler2D gm_BaseTexture;

		layout(location=0) out vec4 pixel;

		void main ()
		{
			vec4 tex1 = texture(gm_BaseTexture, v_v.texture);
			// half lambert'd
			float light = dot(v_v.normal, normalize(vec3(1, 1, 0))) * 0.5 + 0.5;
			vec3 finLight = vec3(max(light, 0.0));

			vec3 baseTexture = texture(gm_BaseTexture, v_v.texture).xyz;
			float shaderz = mix(1.0, v_v.color.x, 0.9);
			vec3 uhh = baseTexture * shaderz;
			uhh = mix(blendOverlay(uhh, vec3(v_v.color.y)), uhh, 0.5);

			pixel = vec4(uhh * finLight, 1.0);
		}
	]]
}