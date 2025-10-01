return {

	vertex = [[
		#version 460 core

		const vec3[] vertices = {
			{ 0, 0, 0.5 },
			{ 0, 1, 0.5 },
			{ 1, 0, 0.5 },
		};

		void main ()
		{
			gl_Position = vec4(vertices[gl_VertexID], 1.0);
		}
	]],
	fragment = [[
		#version 460 core

		layout(location=0) out vec4 pixel;

		void main ()
		{
			pixel = vec4(1, 0, 0, 1);
		}
	]]
}