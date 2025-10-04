package coyote

import org.joml.Vector3d

open class SceneObject
{
	var location = Vector3d()

	open var visible: Boolean = true
	var renderPriority = 0

	open fun step (scene: Scene)
	{
	}

	open fun draw (scene: Scene)
	{
	}

}