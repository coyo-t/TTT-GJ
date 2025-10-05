local nummers = {}
for i = 1, 255 do
	table.insert(nummers, i)
end

return {
	type = 'love',
	space_is_visible = false,
	source = 'texture/font/kfont2.png',
	chars = utf8.char(table.unpack(nummers)),
	margin = 0.00001,
}