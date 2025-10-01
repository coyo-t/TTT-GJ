package coyote


import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack.stackPush

const val INITIAL_TITLE = "MACHINE WITNESS"
const val INITIAL_WIDE = 650
const val INITIAL_TALL = 450

fun getWindowManagerError () = stackPush().use { stack ->
	val name = stack.mallocPointer(1)
	val errc = glfwGetError(name)
	name.stringASCII to errc
}

fun main (vararg args: String)
{
	check(glfwInit()) {
		val (n,_) = getWindowManagerError()
		"glfw failed to init ($n)"
	}

	val windowSize = Vector2i(INITIAL_WIDE, INITIAL_TALL)

	glfwDefaultWindowHints()
	glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
	glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
	glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

	glfwGetVideoMode(glfwGetPrimaryMonitor())?.let { l ->
		val w = l.width()
		val h = l.height()
		glfwWindowHint(GLFW_POSITION_X, (w - windowSize.x) / 2)
		glfwWindowHint(GLFW_POSITION_Y, (h - windowSize.y) / 2)
	}

	glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
	glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

	val windowHandle = glfwCreateWindow(windowSize.x, windowSize.y, INITIAL_TITLE, 0L, 0L).also {
		check(it != 0L) {
			val (n,_) = getWindowManagerError()
			"failed to create window ($n)"
		}
	}

	glfwMakeContextCurrent(windowHandle)
	GL.createCapabilities()

	println(":)")

	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		glfwPollEvents()

		glfwSwapBuffers(windowHandle)
	}

	glfwTerminate()
}
