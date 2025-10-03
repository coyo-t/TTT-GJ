package coyote

import org.joml.Vector2ic
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush

object WindowManager
{
	private var initialized = false
	fun init ()
	{
		check(!initialized) { "already initialized" }
		check(glfwInit()) {
			val (pp, ec) = getError()
			"glfw init fail id $pp '$ec'"
		}
		initialized = true
	}

	fun getError () = stackPush().use { stack ->
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

	fun createWindow (title:String, size:Vector2ic): Long
	{
		return glfwCreateWindow(size.x(), size.y(), title, 0L, 0L).also {
			check(it != 0L) {
				val (e,_) = getError()
				"failed to create window '$e'"
			}
		}
	}

	fun nHint (what:Int, value:Int)
	{
		glfwWindowHint(what, value)
	}

	fun hint (h: Hint.OfDefault)
	{
		glfwDefaultWindowHints()
	}

	fun hint (h: Hint.OfBoolean, value: Boolean)
	{
		nHint(h.id, if (value) GLFW_TRUE else GLFW_FALSE)
	}

	fun hint (h: Hint.OfInt, value: Int)
	{
		nHint(h.id, value)
	}

	fun hint (h: Hint.OfOptionalInt, value: Int?)
	{
		nHint(h.id, value ?: GLFW_DONT_CARE)
	}


	object Hint
	{
		sealed class WindowHint(val id: Int)

		class OfDefault: WindowHint(-1)
		class OfInt(id: Int): WindowHint(id)
		class OfOptionalInt(id: Int): WindowHint(id)
		class OfBoolean(id: Int): WindowHint(id)

		val Defaults = OfDefault()
		val Resizable = OfBoolean(GLFW_RESIZABLE)
		val Visible = OfBoolean(GLFW_VISIBLE)
		val MajorContextVersion = OfInt(GLFW_CONTEXT_VERSION_MAJOR)
		val MinorContextVersion = OfInt(GLFW_CONTEXT_VERSION_MINOR)
		val OpenGLProfile = OfInt(GLFW_OPENGL_PROFILE)
		val LocationX = OfInt(GLFW_POSITION_X)
		val LocationY = OfInt(GLFW_POSITION_Y)
	}
}