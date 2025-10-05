package coyote

import org.joml.Vector3d

class AABB
{

	var x0 = 0.0
	var y0 = 0.0
	var z0 = 0.0
	var x1 = 1.0
	var y1 = 1.0
	var z1 = 1.0


	fun getMinCorner (to: Vector3d): Vector3d
	{
		return to.set(x0, y0, z0)
	}

	fun getMaxCorner (to: Vector3d): Vector3d
	{
		return to.set(x1, y1, z1)
	}

}