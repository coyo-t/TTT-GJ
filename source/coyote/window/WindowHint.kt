package coyote.window

import org.lwjgl.glfw.GLFW

sealed class WindowHint(val id: Int) {

	class OfDefault: WindowHint(-1)
	class OfInt(id: Int): WindowHint(id)
	class OfOptionalInt(id: Int): WindowHint(id)
	class OfBoolean(id: Int): WindowHint(id)

	companion object
	{
		val Defaults = OfDefault()
		val Resizable = OfBoolean(GLFW.GLFW_RESIZABLE)
		val Visible = OfBoolean(GLFW.GLFW_VISIBLE)
		val MajorContextVersion = OfInt(GLFW.GLFW_CONTEXT_VERSION_MAJOR)
		val MinorContextVersion = OfInt(GLFW.GLFW_CONTEXT_VERSION_MINOR)
		val OpenGLProfile = OfInt(GLFW.GLFW_OPENGL_PROFILE)
		val LocationX = OfInt(GLFW.GLFW_POSITION_X)
		val LocationY = OfInt(GLFW.GLFW_POSITION_Y)
	}
}
