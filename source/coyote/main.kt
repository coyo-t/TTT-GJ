package coyote

import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString

const val INITIAL_TITLE = "MACHINE WITNESS"
const val INITIAL_WIDE = 650
const val INITIAL_TALL = 450

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"


fun main (vararg args: String)
{
	// this is precarious >:/
	val LP = DATA_PATH/"dll/"
	loadSystemLibrary(LP/"lua5464.dll")
	org.lwjgl.system.Configuration.LIBRARY_PATH.set((LP/"org/lwjgl").normalize().toAbsolutePath().invariantSeparatorsPathString)

	val RESOURCES = ResourceManager(ASSETS_PATH)
	val SHADERZ = CompiledShaders(RESOURCES)

	val TEST_SHADER = ResourceLocation.of("shader/test auto.lua")

	check(glfwInit()) {
		val (n,_) = getWindowManagerError()
		"glfw failed to init ($n)"
	}

	val windowSize = Vector2i(INITIAL_WIDE, INITIAL_TALL)
	val pevWindowSize = Vector2i(windowSize)

	glfwDefaultWindowHints()
	glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
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

	glfwSetWindowSizeCallback(windowHandle) { _, w, h ->
		windowSize.set(w, h)
	}

	println(":)")

	glEnable(GL_DEBUG_OUTPUT)
	glDebugMessageCallback(::rendererDebugMessage, 0L)

	val pipeline = SHADERZ[TEST_SHADER]

	val dummy = glCreateVertexArrays()

	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		glfwPollEvents()

		glViewport(0, 0, windowSize.x, windowSize.y)
		glClearNamedFramebufferfv(0, GL_COLOR, 0, floatArrayOf(0.5f, 0.1f, 0.4f, 0.0f))
		glClearNamedFramebufferfv(0, GL_DEPTH, 0, floatArrayOf(0.0f))

		pipeline.bind()
		glBindVertexArray(dummy)
		glDrawArrays(GL_TRIANGLES, 0, 3)

		glfwSwapBuffers(windowHandle)
		pevWindowSize.set(windowSize)
	}

	glfwTerminate()
}
