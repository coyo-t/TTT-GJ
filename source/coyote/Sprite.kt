package coyote

import coyote.resource.ResourceLocation

class Sprite(
	val name: ResourceLocation,
	val wide: Int,
	val tall: Int,
	val subImages: List<SubImage>,
)
{

	class SubImage(
		val index: Int,
		val x: Int,
		val y: Int,
	)

}