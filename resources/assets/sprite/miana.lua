

local CW = 15
local CH = 24
local SIZE = { CW, CH }
local page = 'texture/sprite/miana walk.png'

local function sprr (d, x)
	local subs = {}
	local sprx = x * CW
	for i=0, 3 do
		subs[i] = {
			patch = { sprx, i*CH },
		}
	end

	add_sprite {
		name = 'miana idle towards '..d,
		source = page,
		size = SIZE,
		sub_images = {
			[0] = subs[0],
		},
	}

	add_sprite {
		name = 'miana walk towards '..d,
		source = page,
		size = SIZE,
		sub_images = subs,
	}
end

sprr(0, 'west')
sprr(1, 'south')
sprr(2, 'east')
sprr(3, 'north')


