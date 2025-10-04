package coyote

import org.joml.Matrix4f

class SceneObject
{
	var step: ((Scene) -> Boolean)? = null
	var prepareForRender: ((Scene)-> Boolean)? = null
	var applyRenderTransform: ((Scene, Matrix4f)->Unit)? = null
	var draw: ((Scene)->Unit)? = null
}