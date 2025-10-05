return {



	vertex = [[
		#version 460 core

		layout(location=0) in vec3 aLocation;
		layout(location=1) in vec2 aTexture;
		layout(location=2) in vec4 aColor;

		layout(std140, binding=0) uniform MATRICES {
			mat4 matrixProjection;
			mat4 matrixView;
			mat4 matrixWorld;
		};

		layout(location=0) out struct {
			vec2 texture;
			vec4 color;
		} v_v;

		void main ()
		{
			gl_Position = matrixProjection * matrixView * matrixWorld * vec4(aLocation, 1.0);
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