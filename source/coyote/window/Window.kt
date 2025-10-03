package coyote.window

import org.lwjgl.glfw.GLFW.*

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