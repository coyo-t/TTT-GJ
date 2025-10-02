package coyote

import org.joml.Matrix3fStack
import org.joml.Matrix4fStack
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d
import org.lwjgl.opengl.GL45C.*

class Tesselator
{
	private var tesselating = false

	private var f = createBuffer(0x1000)

	val vertexTransform = Matrix4fStack(32)
	val textureTransform = Matrix3fStack(16)

	private val currentColor = Vector4d()
	private val currentNormal = Vector3d()
	private val currentUV0 = Vector2d()
	private val currentUV1 = Vector2d()


	private fun checkAndChangeState (to: Boolean)
	{
		check(tesselating != to) {
			if (tesselating)
				"already building"
			else
				"not building"
		}
		tesselating = to
	}

	fun begin ()
	{
		checkAndChangeState(true)
	}


	fun end ()
	{
		checkAndChangeState(false)
	}


//	val vbo: Int
//	val ibo: Int
//	val vao: Int
//
//	init
//	{
//		vbo = glCreateBuffers()
//		ibo = glCreateBuffers()
//		vao = glCreateVertexArrays()
//	}

}