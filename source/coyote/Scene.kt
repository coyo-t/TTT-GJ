package coyote

import org.joml.Matrix4f

class Scene
{
	val objects = mutableListOf<SceneObject>()
	var viewpoint: SceneObject? = null
	var viewpointShouldBeRendered = false

	private val thingsToBeRendered = mutableListOf<SceneObject>()

	fun prepareObjectsForRendering ()
	{
		thingsToBeRendered.clear()
		for (o in objects)
		{
			val ren = o.prepareForRender ?: continue
			if (ren.invoke(this))
			{
				if (o === viewpoint && !viewpointShouldBeRendered)
				{
					continue
				}
				thingsToBeRendered += o
			}
		}
	}

	fun applyViewpointTransform (to: Matrix4f)
	{
		viewpoint?.let {
			it.applyRenderTransform?.invoke(this, to)
			to.invert()
		}
	}

	fun renderObjects ()
	{
		for (o in objects)
		{
			drawGlobalTransform.pushMatrix()
			o.applyRenderTransform?.invoke(this, drawGlobalTransform)
			o.draw?.invoke(this)
			drawGlobalTransform.popMatrix()
		}
	}
}