package coyote.window

import org.joml.Vector2ic
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.system.MemoryStack

object WindowManager
{
	private var initialized = false

	val canUseRawMouseAcceleration by lazy {
		glfwRawMouseMotionSupported()
	}

	var time: Double
		get() = glfwGetTime()
		set(v) {
			glfwSetTime(v)
		}

	val primaryMonitor: Long? get() {
		val o = glfwGetPrimaryMonitor()
		return if (o == 0L) null else o
	}

	fun getVideoMode (monitor: Long?): GLFWVidMode?
	{
		if (monitor == null)
			return null
		if (monitor == 0L)
			return null
		return glfwGetVideoMode(monitor)
	}

	fun init ()
	{
		check(!initialized) { "already initialized" }
		check(glfwInit()) {
			val (pp, ec) = getError()
			"glfw init fail id $pp '$ec'"
		}
		initialized = true
	}

	fun getError () = MemoryStack.stackPush().use { stack ->
		val name = stack.mallocPointer(1)
		val errc = glfwGetError(name)
		name.stringASCII to errc
	}

	fun close ()
	{
		check(initialized) { "not initialized" }
		glfwTerminate()
		initialized = false
	}

	fun pollEvents ()
	{
		glfwPollEvents()
	}

	fun createWindow (title:String, size: Vector2ic): Window
	{
		val out = glfwCreateWindow(size.x(), size.y(), title, 0L, 0L)
		check(out != 0L) {
			val (e,_) = getError()
			"failed to create window '$e'"
		}
		return Window(out)
	}

	fun nHint (what:Int, value:Int)
	{
		glfwWindowHint(what, value)
	}

	fun hint (h: WindowHint.OfDefault)
	{
		glfwDefaultWindowHints()
	}

	fun hint (h: WindowHint.OfBoolean, value: Boolean)
	{
		nHint(h.id, if (value) GLFW_TRUE else GLFW_FALSE)
	}

	fun hint (h: WindowHint.OfInt, value: Int)
	{
		nHint(h.id, value)
	}

	fun hint (h: WindowHint.OfOptionalInt, value: Int?)
	{
		nHint(h.id, value ?: GLFW_DONT_CARE)
	}


}