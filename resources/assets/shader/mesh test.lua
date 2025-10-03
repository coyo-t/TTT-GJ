return {
	vertex = [[
		#version 460 core

		layout(location=0) in vec3 aLocation;
		layout(location=1) in vec2 aTexture;
		layout(location=2) in vec3 aNormal;
		layout(location=3) in vec4 aColor;

		layout(std140, binding=0) uniform MATRICES {
			mat4 matrixPVM;
			mat4 matrix1;
			mat4 matrix2;
		};

		layout(location=0) out struct {
			vec2 texture;
			vec4 color;
		} v_v;

		void main ()
		{
			gl_Position = matrixPVM * vec4(aLocation, 1.0);
			v_v.texture = aTexture;
			v_v.color = aColor;
		}
	]],
	fragment = [[
		#version 460 core

		layout(location=0) in struct {
			vec2 texture;
			vec4 color;
		} v_v;

		layout(binding=0) uniform sampler2D gm_BaseTexture;

		layout(location=0) out vec4 pixel;

		void main ()
		{
			pixel = texture(gm_BaseTexture, v_v.texture) * v_v.color;
		}
	]]
}