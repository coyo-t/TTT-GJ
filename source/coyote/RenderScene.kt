package coyote

class RenderScene
{
	val objects = mutableListOf<RenderObject>()
	var viewpoint: RenderObject? = null
	var viewpointShouldBeRendered = false

	private val thingsToBeRendered = mutableListOf<RenderObject>()



}