return {

	vertex = [[
		#version 460 core

		layout(location=0) in vec3 aLocation;
		layout(location=1) in vec2 aTexture;
		layout(location=2) in vec4 aColor;

		out vec2 vTexture;
		out vec4 vColor;

		void main ()
		{
			gl_Position = vec4(aLocation, 1.0);
			vTexture = aTexture;
			vColor = aColor;
		}
	]],
	fragment = [[
		#version 460 core

		in vec2 vTexture;
		in vec4 vColor;

		layout(location=0) out vec4 pixel;

		void main ()
		{
			pixel = vColor;
		}
	]]
}