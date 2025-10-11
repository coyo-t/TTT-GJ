-- TODO named uniform references/usages
local function MAT (oname, tex, sha)
	if sha == nil then
		sha = 'prop.lua'
	end
	sha = 'shader/'..sha
	return {
		obj_name = oname,
		shader = sha,
		textures = {
			[0] = 'texture/'..tex
		}
	}
end

return {
	mesh = {
		data = [[
			# Blender 4.5.3 LTS
			# www.blender.org
			mtllib dingus machine.mtl
			o Cube.019
			v -1.000000 0.000000 0.000000
			v -0.999999 1.474412 0.000000
			v 1.000000 1.474412 0.000000
			v -0.999999 1.384413 1.000000
			v 1.000000 1.384412 1.000000
			v 1.000000 0.000000 0.000000
			v -1.000000 0.500000 1.000000
			v 1.000000 0.500000 1.000000
			vn -0.0000 -0.8944 0.4472
			vn 1.0000 -0.0000 -0.0000
			vn -1.0000 -0.0000 -0.0000
			vn -0.0000 -0.0000 1.0000
			vt 0.531999 0.021401
			vt 0.052068 0.557980
			vt 0.052069 0.021401
			vt 0.596116 0.981994
			vt 0.921069 0.923502
			vt 0.921069 0.348717
			vt 0.596116 0.981992
			vt 0.596116 0.023763
			vt 0.921069 0.348717
			vt 0.052068 0.982436
			vt 0.531999 0.557980
			vt 0.531999 0.982437
			vt 0.596116 0.023764
			vt 0.921069 0.923501
			s 0
			usemtl Material.007
			f 6/1/1 7/2/1 1/3/1
			f 3/4/2 5/5/2 8/6/2
			f 2/7/3 1/8/3 7/9/3
			f 4/10/4 8/11/4 5/12/4
			f 6/13/2 3/4/2 8/6/2
			f 4/14/3 2/7/3 7/9/3
			f 6/1/1 8/11/1 7/2/1
			f 4/10/4 7/2/4 8/11/4
		]],
	},
	materials = {
		MAT('Material.007', 'prop/wall thing.kra'),
	},
}
