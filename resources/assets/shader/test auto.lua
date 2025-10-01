return {

	vertex = [[
		#version 450 core

		const vec3[] vertices = {
			{ 0, 0, 0 },
			{ 1, 0, 0 },
			{ 0, 1, 0 },
		};

		void main ()
		{
			gl_Position = vec4(vertices[gl_VertexId], 1.0);
		}
	]],
	fragment = [[
		#version 450 core

		out vec4 pixel;

		void main ()
		{
			pixel = vec4(1.0, 0.0, 0.8, 1.0);
		}
	]]
}