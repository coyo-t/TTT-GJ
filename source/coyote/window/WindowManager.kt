package coyote.window

import org.joml.Vector2ic
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack

object WindowManager
{
	private var initialized = false

	val canUseRawMouseAcceleration by lazy {
		GLFW.glfwRawMouseMotionSupported()
	}

	var time: Double
		get() = GLFW.glfwGetTime()
		set(v) {
			GLFW.glfwSetTime(v)
		}

	fun init ()
	{
		check(!initialized) { "already initialized" }
		check(GLFW.glfwInit()) {
			val (pp, ec) = getError()
			"glfw init fail id $pp '$ec'"
		}
		initialized = true
	}

	fun getError () = MemoryStack.stackPush().use { stack ->
		val name = stack.mallocPointer(1)
		val errc = GLFW.glfwGetError(name)
		name.stringASCII to errc
	}

	fun close ()
	{
		check(initialized) { "not initialized" }
		GLFW.glfwTerminate()
		initialized = false
	}

	fun pollEvents ()
	{
		GLFW.glfwPollEvents()
	}

	fun createWindow (title:String, size: Vector2ic): Window
	{
		val out = GLFW.glfwCreateWindow(size.x(), size.y(), title, 0L, 0L)
		check(out != 0L) {
			val (e,_) = getError()
			"failed to create window '$e'"
		}
		return Window(out)
	}

	fun nHint (what:Int, value:Int)
	{
		GLFW.glfwWindowHint(what, value)
	}

	fun hint (h: WindowHint.OfDefault)
	{
		GLFW.glfwDefaultWindowHints()
	}

	fun hint (h: WindowHint.OfBoolean, value: Boolean)
	{
		nHint(h.id, if (value) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
	}

	fun hint (h: WindowHint.OfInt, value: Int)
	{
		nHint(h.id, value)
	}

	fun hint (h: WindowHint.OfOptionalInt, value: Int?)
	{
		nHint(h.id, value ?: GLFW.GLFW_DONT_CARE)
	}


}