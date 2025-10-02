return {

	vertex = [[
		#version 460 core

		const vec3[] vertices = {
			{ -1, -1, 0 },
			{ -1, +2+1.41421356237, 0 },
			{ +2+1.41421356237, -1, 0 },
		};

		const vec2[] uvs = {
			{ 0, 0 },
			{ 0, 2 },
			{ 2, 0 },
		};

		layout(location=0) out vec2 v_vTexcoord;

		void main ()
		{
			gl_Position = vec4(vertices[gl_VertexID], 1.0);
			v_vTexcoord = uvs[gl_VertexID];
		}
	]],
	fragment = [[
		#version 460 core

		layout(location=0) in vec2 v_vTexcoord;
		layout(location=0) out vec4 pixel;

		void main ()
		{
			pixel = vec4(v_vTexcoord.x, v_vTexcoord.y, 0, 1);
		}
	]]
}