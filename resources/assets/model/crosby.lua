-- TODO named uniform references/usages
local function MAT (oname, tex, sha)
	if sha == nil then
		sha = 'crosby.lua'
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
	mesh = 'mesh/crosby.obj',
	materials = {
		MAT('debugminicrosby', 'untextured.png'),
	},
}
