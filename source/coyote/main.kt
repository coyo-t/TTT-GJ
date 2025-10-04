package coyote

import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import coyote.window.WindowHint
import coyote.window.WindowManager
import org.joml.Math.lerp
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color
import java.lang.foreign.Arena
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val INITIAL_TITLE = "MACHINE WITNESS"
const val INITIAL_WIDE = 650
const val INITIAL_TALL = 450

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"

val TEXTURE_WHITE = ResourceLocation.of("texture/white")

val TEST_VERTEX_FORMAT = buildVertexFormat {
	location3D()
	textureCoord()
	byteColor()
}
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

	WindowManager.hint(WindowHint.Defaults)
	WindowManager.hint(WindowHint.MajorContextVersion, 4)
	WindowManager.hint(WindowHint.MinorContextVersion, 6)
	WindowManager.hint(WindowHint.OpenGLProfile, GLFW_OPENGL_CORE_PROFILE)

	glfwGetVideoMode(glfwGetPrimaryMonitor())?.let { l ->
		val w = l.width()
		val h = l.height()
		WindowManager.hint(WindowHint.LocationX, (w - windowSize.x) / 2)
		WindowManager.hint(WindowHint.LocationY, (h - windowSize.y) / 2)
	}

	WindowManager.hint(WindowHint.Resizable, true)
	WindowManager.hint(WindowHint.Visible, false)

	val window = WindowManager.createWindow(INITIAL_TITLE, windowSize)
	window.makeContextCurrent()

	window.setSizeLimits(
		minSize = 320 to 240,
		maxSize = null,
	)
	window.setRawMouseMotion(true)

	val pevMouseCo = Vector2d()
	val curMouseCo = Vector2d()
	val mouseDelta = Vector2d()
	var windowHasFocus = true

	glfwSetWindowSizeCallback(window.handle) { _, w, h ->
		windowSize.set(w, h)
	}

	glfwSetWindowFocusCallback(window.handle) { _, focused ->
		windowHasFocus = focused
	}

	println(":)")

	drawCreateCapabilities()
	drawSetState(GL_DEBUG_OUTPUT, true)
	drawSetDebugMessageCallback(0L, ::rendererDebugMessage)

	val shaderTest_uniformBlocks = SHADERZ[ResourceLocation.of("shader/test.lua")]
	val shaderTest_storedOBJ = SHADERZ[ResourceLocation.of("shader/mesh test.lua")]

	val textureTest_labyrinthOctoEnv = TEXTUREZ[ResourceLocation.of("texture/env/fpw mk2 labyrinth alpha.png")]
	val textureTest_screenTri = TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]

//	val surface = glCreateFramebuffers()

	Arena.ofConfined().use { arena ->
		val pm = arena.allocate(16*16*4)
		pm.fill(0xFF.toByte())
		val pic = NativeImage(16, 16, pm)
		TEXTUREZ.add(TEXTURE_WHITE, pic)
	}

	val testSavedModel = MODELZ[ResourceLocation.of("model/octmeshprev.obj")]


	val inputMap = mutableMapOf<Int, Boolean>().withDefault { false }
	val inputVec = Vector3d()
	val viewCo = Vector3d()
	var viewPitch = 0.0
	var viewYaw = 0.0
	var viewSens = 1.0 / 8.0
	var mouseGrabbed = false

	fun mouseGrabbedness (gr: Boolean)
	{
		mouseGrabbed = gr
		if (gr)
			window.setCursorMode(GLFW_CURSOR_DISABLED)
		else
			window.setCursorMode(GLFW_CURSOR_NORMAL)
	}

	glfwSetKeyCallback(window.handle) { _, key, scancode, action, mods ->
		if (action == GLFW_PRESS)
			inputMap[key] = true
		else if (action == GLFW_RELEASE)
			inputMap[key] = false
		when (key)
		{
			GLFW_KEY_ESCAPE if (action == GLFW_PRESS) -> mouseGrabbedness(!mouseGrabbed)
		}
	}

	val testRenderTargetTexture = TEXTUREZ.createTexture(256, 256).apply {
		setFilter(GL_LINEAR, GL_LINEAR)
	}
	val testSurface = drawCreateSurface().apply {
		setColorAttachment(0, testRenderTargetTexture)
		setDepthAttachment(256, 256)
	}

	val rtTexDisplayTestUhhh = TEXTUREZ[ResourceLocation.of("texture/color calibration card.kra")]

	drawSetDepthCompareFunc(GL_LESS)

	var time = WindowManager.time
	var pevTime = time
	window.show()
	while (!window.shouldClose)
	{
		pevTime = time
		val time = WindowManager.time
		val deltaTime = time - pevTime
		pevMouseCo.set(curMouseCo)
		WindowManager.pollEvents()

		stackPush().use { stack ->
			val x = stack.mallocDouble(1)
			val y = stack.mallocDouble(1)
			glfwGetCursorPos(window.handle, x, y)
			curMouseCo.set(x.get(), y.get())
			curMouseCo.sub(pevMouseCo, mouseDelta)
		}
		if (!windowHasFocus)
			mouseGrabbedness(false)

		if (mouseGrabbed)
		{
			viewPitch = (viewPitch + mouseDelta.y * viewSens).clampedSym(90.0)
			viewYaw = viewYaw + mouseDelta.x * viewSens

			inputVec.set(0.0)
			inputVec.x += if (inputMap[GLFW_KEY_D] == true) 1.0 else 0.0
			inputVec.x -= if (inputMap[GLFW_KEY_A] == true) 1.0 else 0.0
			inputVec.z -= if (inputMap[GLFW_KEY_W] == true) 1.0 else 0.0
			inputVec.z += if (inputMap[GLFW_KEY_S] == true) 1.0 else 0.0
			inputVec.y += if (inputMap[GLFW_KEY_SPACE] == true) 1.0 else 0.0
			inputVec.y -= if (inputMap[GLFW_KEY_LEFT_SHIFT] == true) 1.0 else 0.0
			inputVec.rotateY(-viewYaw.toRadians())
			inputVec.mulAdd(deltaTime * 0.001, viewCo, viewCo)
		}


		val winWide = windowSize.x
		val winTall = windowSize.y
		drawToSurface(testSurface) {
			val (wide,tall) = testRenderTargetTexture.size
			drawSetViewPort(wide, tall)
			drawSetDepthTestEnable(true)
			drawSetDepthWriteEnable(true)
			drawGlobalTransform.apply {
				identity()
				ortho(0f, wide.toFloat(), tall.toFloat(), 0f, 0f, 10f)
				translate(128f, 128f, -1f)
				rotateZ((sin(time * PI) * 45.0).toRadiansf())
				scale(64f)
			}
			val r = lerp(sin(time * 0.9 * PI) * 0.5 + 0.5, 0.2, 0.8)
			val g = lerp(cos(time * 0.8 * PI) * 0.5 + 0.5, 0.3, 0.9)
			val b = 0.7
			drawClearColor(r,g,b)
			drawClearDepth(1)
			drawSetShader(shaderTest_uniformBlocks)
			drawBindTexture(0, rtTexDisplayTestUhhh)
			drawMesh(TEST_VERTEX_FORMAT, GL_TRIANGLES) { tess -> with(tess) {
//				vertexTransform.scale(256.0)
				color(Color.WHITE)
				vertex(-0.5,-0.5,0, 0,0)
				vertex(+0.5,-0.5,0, 1,0)
				vertex(+0.5,+0.5,0, 1,1)
				vertex(-0.5,+0.5,0, 0,1)
				quad()
			}}
		}

		drawSetSurface(null)

		drawSetDepthTestEnable(true)
		drawSetDepthWriteEnable(true)
		drawSetViewPort(winWide, winTall)
		drawClearDepth(1)
		drawGlobalTransform.apply {
			identity()
			perspective(70f, winWide.toFloat()/winTall, 0.001f, 100f)
			rotateX(viewPitch.toRadiansf())
			rotateY(viewYaw.toRadiansf())
			translate(-viewCo.x.toFloat(), -viewCo.y.toFloat(), -viewCo.z.toFloat())
		}

		drawSetShader(shaderTest_storedOBJ)
		drawBindTexture(0, textureTest_labyrinthOctoEnv)
		drawBindTexture(1, testRenderTargetTexture)
		drawSubmit(testSavedModel, GL_TRIANGLES)

		drawSetShader(shaderTest_uniformBlocks)
		drawBindTexture(0, TEXTUREZ[TEXTURE_WHITE])
		drawSubmit(modelTest_compass, GL_LINES)

		drawBlitSurfaces(
			testSurface, 0, 0, 256, 256,
			null, 32, 32, 128, 128,
			GL_COLOR_BUFFER_BIT,
			GL_LINEAR,
		)

		window.swapBuffers()
		pevWindowSize.set(windowSize)
	}

	WindowManager.close()
}
