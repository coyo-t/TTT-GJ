package coyote

class Scene
{
	val objects = mutableListOf<SceneObject>()

	fun stepObjects ()
	{
		for (o in objects)
		{
			o.step(this)
		}
	}

	fun renderObjects ()
	{
		objects.sortBy { it.renderPriority }
		for (o in objects)
		{
			if (!o.visible)
				continue
			o.draw(this)
		}
	}
}