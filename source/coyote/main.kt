package coyote

import coyote.geom.TesselatorStore
import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Matrix4fStack
import org.joml.Vector2d
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString

const val INITIAL_TITLE = "MACHINE WITNESS"
const val INITIAL_WIDE = 650
const val INITIAL_TALL = 450

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"

val TEXTURE_WHITE = ResourceLocation.of("texture/white")

val TEST_VERTEX_FORMAT = buildVertexFormat {
	location3D()
	uv(0)
	byteColor()
}

fun main (vararg args: String)
{
	// this is precarious >:/
	val LP = DATA_PATH/"dll/"
	loadSystemLibrary(LP/"lua5464.dll")
	Configuration.LIBRARY_PATH.set((LP/"org/lwjgl").normalize().toAbsolutePath().invariantSeparatorsPathString)

	val RESOURCES = ResourceManager(ASSETS_PATH)
	val SHADERZ = CompiledShaders(RESOURCES)
	val TEXTUREZ = TextureManager(RESOURCES)
	val MODELZ = OBJModelManager(RESOURCES)

	WindowManager.init()

	val windowSize = Vector2i(INITIAL_WIDE, INITIAL_TALL)
	val pevWindowSize = Vector2i(windowSize)

	WindowManager.hint(WindowManager.Hint.Defaults)
	WindowManager.hint(WindowManager.Hint.MajorContextVersion, 4)
	WindowManager.hint(WindowManager.Hint.MinorContextVersion, 6)
	WindowManager.hint(WindowManager.Hint.OpenGLProfile, GLFW_OPENGL_CORE_PROFILE)

	glfwGetVideoMode(glfwGetPrimaryMonitor())?.let { l ->
		val w = l.width()
		val h = l.height()
		WindowManager.hint(WindowManager.Hint.LocationX, (w - windowSize.x) / 2)
		WindowManager.hint(WindowManager.Hint.LocationY, (h - windowSize.y) / 2)
	}

	WindowManager.hint(WindowManager.Hint.Resizable, true)
	WindowManager.hint(WindowManager.Hint.Visible, false)

	val windowHandle = WindowManager.createWindow(INITIAL_TITLE, windowSize)

	glfwSetWindowSizeLimits(windowHandle, 320, 240, GLFW_DONT_CARE, GLFW_DONT_CARE)

	glfwMakeContextCurrent(windowHandle)
	GL.createCapabilities()

	glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE)

	val pevMouseCo = Vector2d()
	val curMouseCo = Vector2d()
	val mouseDelta = Vector2d()
	var windowHasFocus = true

	glfwSetWindowSizeCallback(windowHandle) { _, w, h ->
		windowSize.set(w, h)
	}

	glfwSetWindowFocusCallback(windowHandle) { _, focused ->
		windowHasFocus = focused
	}

	println(":)")

	glEnable(GL_DEBUG_OUTPUT)
	glDebugMessageCallback(::rendererDebugMessage, 0L)

	val shaderTest_uniformBlocks = SHADERZ[ResourceLocation.of("shader/test.lua")]
	val shaderTest_storedOBJ = SHADERZ[ResourceLocation.of("shader/mesh test.lua")]

	val matrixBuffer = glCreateBuffers()
	val matrixBufferSize = ((4*4)*4L)*3
	glNamedBufferStorage(
		matrixBuffer,
		matrixBufferSize,
		GL_DYNAMIC_STORAGE_BIT,
	)

	val matrixSegment = Arena.ofAuto().allocate(matrixBufferSize)
	matrixSegment.setAtIndex(JAVA_FLOAT, 0L, 1f)
	matrixSegment.setAtIndex(JAVA_FLOAT, 5L, 1f)
	matrixSegment.setAtIndex(JAVA_FLOAT, 11L, 1f)
	matrixSegment.setAtIndex(JAVA_FLOAT, 15L, 1f)

	val textureTest_labyrinthOctoEnv = TEXTUREZ[ResourceLocation.of("texture/env/fpw mk2 labyrinth alpha.png")]
	val textureTest_screenTri = TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]

//	val surface = glCreateFramebuffers()

	Arena.ofConfined().use { arena ->
		val pm = arena.allocate(16*16*4)
		pm.fill(0xFF.toByte())
		val pic = NativeImage(16, 16, pm)
		TEXTUREZ.add(TEXTURE_WHITE, pic)
	}

	val transform = Matrix4fStack(16)
	val testSavedModel = MODELZ[ResourceLocation.of("model/octmeshprev.obj")]
	val modelTest_compass = buildModel(TEST_VERTEX_FORMAT) {
		vertexTransform.apply {
			scale(0.1)
		}
		// north
		color(Color(0xD23636))
		startLine()
		vertex(-0.1, -0.1, -1)
		vertex(-0.1, +0.1, -1)
		vertex(+0.1, +0.1, -1)
		vertex(+0.1, -0.1, -1)
		endLine(true)

		// east
		color(Color(0xFFD240))
		startLine()
		vertex(+1, -0.1, -0.1)
		vertex(+1, +0.1, -0.1)
		vertex(+1, +0.1, +0.1)
		vertex(+1, -0.1, +0.1)
		endLine(true)

		// up
		color(Color(0x67A6FC))
		startLine()
		vertex(-0.1, +1, -0.1)
		vertex(-0.1, +1, +0.1)
		vertex(+0.1, +1, +0.1)
		vertex(+0.1, +1, -0.1)
		endLine(true)
	}

	var viewPitch = 0.0
	var viewYaw = 0.0
	var viewSens = 1.0 / 8.0
	var mouseGrabbed = false

	fun mouseGrabbedness (gr: Boolean)
	{
		mouseGrabbed = gr
		if (gr)
			glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
		else
			glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
	}

	glfwSetKeyCallback(windowHandle) { _, key, scancode, action, mods ->
		if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS)
			mouseGrabbedness(!mouseGrabbed)
	}

	var currentShader: CompiledShaders.ShaderPipeline? = null
	fun submit (tess: TesselatorStore, pr:Int)
	{
		val currentShader = currentShader ?: return
		transform.get(matrixSegment, 0L)
		nglNamedBufferSubData(matrixBuffer, 0L, matrixBufferSize, matrixSegment.address())
		val uhh = glGetUniformBlockIndex(currentShader.vr, "MATRICES")
		glUniformBlockBinding(currentShader.vr, uhh, 0)
		glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
		tess.submit(pr)
	}
	fun useShader (res: CompiledShaders.ShaderPipeline)
	{
		if (res == currentShader)
			return
		res.bind()
		currentShader = res
	}



	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		pevMouseCo.set(curMouseCo)
		WindowManager.pollEvents()

		stackPush().use { stack ->
			val x = stack.mallocDouble(1)
			val y = stack.mallocDouble(1)
			glfwGetCursorPos(windowHandle, x, y)
			curMouseCo.set(x.get(), y.get())
			curMouseCo.sub(pevMouseCo, mouseDelta)
		}
		if (!windowHasFocus)
			mouseGrabbedness(false)

		if (mouseGrabbed)
		{
			viewPitch = (viewPitch + mouseDelta.y * viewSens).clampedSym(90.0)
			viewYaw = viewYaw + mouseDelta.x * viewSens
		}

		val time = WindowManager.time

		val winWide = windowSize.x
		val winTall = windowSize.y
		glViewport(0, 0, winWide, winTall)
//		glClearNamedFramebufferfv(0, GL_COLOR, 0, floatArrayOf(0.5f, 0.1f, 0.4f, 0.0f))
		glClearNamedFramebufferfv(0, GL_DEPTH, 0, floatArrayOf(0.0f))
//		glBlitNamedFramebuffer()

		transform.apply {
			identity()
			perspective(70f, winWide.toFloat()/winTall, 0.001f, 100f)
			rotateX(viewPitch.toRadiansf())
			rotateY(viewYaw.toRadiansf())
		}


		useShader(shaderTest_storedOBJ)
		glBindTextureUnit(0, textureTest_labyrinthOctoEnv.handle)
		submit(testSavedModel, GL_TRIANGLES)

		useShader(shaderTest_uniformBlocks)
		glBindTextureUnit(0, TEXTUREZ[TEXTURE_WHITE].handle)
		submit(modelTest_compass, GL_LINES)

		glfwSwapBuffers(windowHandle)
		pevWindowSize.set(windowSize)
	}

	WindowManager.close()
}
