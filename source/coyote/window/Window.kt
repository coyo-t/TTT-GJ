package coyote.window

import org.joml.Vector2d
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush
import kotlin.use

class Window(val handle: Long)
{

	val shouldClose get() = glfwWindowShouldClose(handle)

	fun setCursorMode (to:Int)
	{
		glfwSetInputMode(handle, GLFW_CURSOR, to)
	}

	fun setRawMouseMotion (to: Boolean)
	{
		glfwSetInputMode(handle, GLFW_RAW_MOUSE_MOTION, if (to) GLFW_TRUE else GLFW_FALSE)
	}

	fun swapBuffers ()
	{
		glfwSwapBuffers(handle)
	}

	fun makeContextCurrent ()
	{
		glfwMakeContextCurrent(handle)
	}

	fun show ()
	{
		glfwShowWindow(handle)
	}

	fun getCursorLocation (into: Vector2d): Vector2d
	{
		stackPush().use { stack ->
			val x = stack.mallocDouble(1)
			val y = stack.mallocDouble(1)
			glfwGetCursorPos(handle, x, y)
			return into.set(x.get(), y.get())
		}
	}

	fun setSizeLimits (minSize:Pair<Int,Int>?, maxSize:Pair<Int,Int>?)
	{
		glfwSetWindowSizeLimits(
			handle,
			minSize?.first ?: GLFW_DONT_CARE,
			minSize?.second ?: GLFW_DONT_CARE,
			maxSize?.first ?: GLFW_DONT_CARE,
			maxSize?.second ?: GLFW_DONT_CARE,
		)
	}
}