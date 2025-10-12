package coyote

import coyote.resource.ResourceLocation

class Sprite(
	val name: ResourceLocation,
	source: ResourceLocation,
	val wide: Int,
	val tall: Int,
	subImages: List<SubImage>,
)
{
	val texture = TEXTUREZ[source]

	private val rcpTexWide = 1.0 / texture.wide
	private val rcpTexTall = 1.0 / texture.tall

	val subImages = subImages.associate {
		it.index to (it to Rectangle.ofSized(
			it.x.toDouble() * rcpTexWide,
			it.y.toDouble() * rcpTexTall,
			wide.toDouble() * rcpTexWide,
			tall.toDouble() * rcpTexTall,
		))
	}

	class SubImage(
		val index: Int,
		val x: Int,
		val y: Int,
		val blank: Boolean = false,
	)

}