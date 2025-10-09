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
	mesh = 'mesh/player space.obj',
	materials = {
		MAT('carpet_not_carpet_separator', 'surface/floor separator.png'),
		MAT('ceiling', 'surface/popcorn ceiling.kra'),
		MAT('floor_carpet', 'surface/floor carpet.kra'),
		MAT('floor_not_carpet', 'surface/tile floor.png'),
		MAT('wall_concrete_thing', 'surface/cracked concrete.png'),
		MAT('wall_lower_rim', 'surface/lower wall rim.png'),
		MAT('wall_main', 'surface/office wall.kra', 'office wall.lua'),
		MAT('wall_upper_rim', 'surface/lower wall rim.png'),
	},
}
