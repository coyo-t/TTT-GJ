package coyote

import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import coyote.window.Window
import coyote.window.WindowHint
import coyote.window.WindowManager
import org.joml.Math.lerp
import org.joml.Matrix4f
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL46C.GL_DEBUG_OUTPUT
import java.awt.Color
import java.lang.foreign.Arena
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FPW: AutoCloseable
{
	val TEXTURE_TRANSPARENT = ResourceLocation.of("texture/transparent")
	val TEXTURE_BLACK = ResourceLocation.of("texture/black")
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

	val RESOURCES = ResourceManager(ASSETS_PATH)
	val SHADERZ = CompiledShaders(RESOURCES)
	val TEXTUREZ = TextureManager(RESOURCES)
	val MODELZ = OBJModelManager(RESOURCES)

	val windowSize = Vector2i(650, 450)
	val pevWindowSize = Vector2i(windowSize)
	val pevMouseCo = Vector2d()
	val curMouseCo = Vector2d()
	val mouseDelta = Vector2d()
	var windowHasFocus = true
	val inputMap = mutableMapOf<Int, Boolean>().withDefault { false }
	val inputVec = Vector3d()
	val viewCo = Vector3d()
	var viewPitch = 0.0
	var viewYaw = 0.0
	var viewSens = 1.0 / 8.0
	var mouseGrabbed = false
	var time = WindowManager.time
	var pevTime = time
	var deltaTime = time - pevTime

	private var firstFrame = true

	val viewpoint = SceneObject()
	val viewMatrix = Matrix4f()

	val scene = Scene().also {
		it.viewpoint = viewpoint
	}


	val window: Window
	init
	{
		window = with(WindowManager) {
			hint(WindowHint.Defaults)
			hint(WindowHint.MajorContextVersion, 4)
			hint(WindowHint.MinorContextVersion, 6)
			hint(WindowHint.OpenGLProfile, GLFW_OPENGL_CORE_PROFILE)

			getVideoMode(primaryMonitor)?.let { l ->
				val w = l.width()
				val h = l.height()
				hint(WindowHint.LocationX, (w - windowSize.x) / 2)
				hint(WindowHint.LocationY, (h - windowSize.y) / 2)
			}

			hint(WindowHint.Resizable, true)
			hint(WindowHint.Visible, false)
			createWindow("MACHINE WITNESS", windowSize)
		}

		window.setSizeLimits(
			minSize = 320 to 240,
			maxSize = null,
		)
	}

	fun mouseGrabbedness (gr: Boolean)
	{
		mouseGrabbed = gr
		if (gr)
			window.setCursorMode(GLFW_CURSOR_DISABLED)
		else
			window.setCursorMode(GLFW_CURSOR_NORMAL)
	}

	val shaderTest_uniformBlocks by lazy {
		SHADERZ[ResourceLocation.of("shader/test.lua")]
	}
	val shaderTest_storedOBJ by lazy {
		SHADERZ[ResourceLocation.of("shader/mesh test.lua")]
	}

	val textureTest_labyrinthOctoEnv by lazy {
		TEXTUREZ[ResourceLocation.of("texture/env/fpw mk2 labyrinth alpha.png")]
	}
	val textureTest_screenTri by lazy {
		TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]
	}
	val testSavedModel by lazy {
		MODELZ[ResourceLocation.of("model/octmeshprev.obj")]
	}

	val testRenderTargetTexture by lazy {
		TEXTUREZ.createTexture(256, 256).apply {
			setFilter(GL_LINEAR, GL_LINEAR)
		}
	}
	val testSurface by lazy {
		drawCreateSurface().apply {
			setColorAttachment(testRenderTargetTexture)
			setDepthAttachment(256, 256)
		}
	}

	val rtTexDisplayTestUhhh by lazy {
		TEXTUREZ[ResourceLocation.of("texture/color calibration card.kra")]
	}

	fun init ()
	{
		scene.objects += viewpoint.apply {
			step = { _ ->
				if (mouseGrabbed)
				{
					viewPitch = (viewPitch + mouseDelta.y * viewSens).clampedSym(90.0)
					viewYaw = viewYaw + mouseDelta.x * viewSens

					inputVec.x += if (inputMap[GLFW_KEY_D] == true) 1.0 else 0.0
					inputVec.x -= if (inputMap[GLFW_KEY_A] == true) 1.0 else 0.0
					inputVec.z -= if (inputMap[GLFW_KEY_W] == true) 1.0 else 0.0
					inputVec.z += if (inputMap[GLFW_KEY_S] == true) 1.0 else 0.0
					inputVec.y += if (inputMap[GLFW_KEY_SPACE] == true) 1.0 else 0.0
					inputVec.y -= if (inputMap[GLFW_KEY_LEFT_SHIFT] == true) 1.0 else 0.0

					if (inputVec.x != 0.0 || inputVec.y != 0.0 || inputVec.z != 0.0)
					{
						inputVec.normalize()
						inputVec.rotateY(-viewYaw.toRadians())
						inputVec.mulAdd(deltaTime * 2.0, viewCo, viewCo)
					}
				}
				true
			}
			applyRenderTransform = { _, mat ->
				with (mat)
				{
					identity()
					translate(viewCo.x.toFloat(), viewCo.y.toFloat(), viewCo.z.toFloat())
					rotateY(-viewYaw.toRadiansf())
					rotateX(-viewPitch.toRadiansf())
				}
			}
		}
		scene.objects += SceneObject().apply {
			// env
			draw = { _ ->
				drawSetShader(shaderTest_storedOBJ)
				drawBindTexture(0, textureTest_labyrinthOctoEnv)
				drawBindTexture(1, testRenderTargetTexture)
				drawSubmit(testSavedModel, GL_TRIANGLES)
			}
		}
		scene.objects += SceneObject().apply {
			// compass
			draw = { _ ->
				drawSetShader(shaderTest_uniformBlocks)
				drawBindTexture(0, TEXTUREZ[TEXTURE_WHITE])
				drawSubmit(modelTest_compass, GL_LINES)
			}
		}
		window.setRawMouseMotion(true)
		glfwSetWindowSizeCallback(window.handle) { _, w, h ->
			windowSize.set(w, h)
			draw()
		}
		glfwSetFramebufferSizeCallback(window.handle) { _, w, h ->
		}

		glfwSetWindowFocusCallback(window.handle) { _, focused ->
			windowHasFocus = focused
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

		window.makeContextCurrent()
		drawCreateCapabilities()
		drawSetFlag(GL_DEBUG_OUTPUT, true)
		drawSetDebugMessageCallback(0L, ::rendererDebugMessage)

		Arena.ofConfined().use { arena ->
			val pm = arena.allocate(16*16*4)
			pm.fill(0xFF.toByte())
			val pic = NativeImage(16, 16, pm)
			TEXTUREZ.add(TEXTURE_WHITE, pic)
		}
		drawSetDepthCompareFunc(GL_LESS)
		drawSetCullingSide(GL_BACK)
		drawSetCullingEnabled(true)
	}

	fun draw ()
	{
		val winWide = windowSize.x
		val winTall = windowSize.y
		drawToSurface(testSurface) {
			val (wide, tall) = testRenderTargetTexture.size
			drawSetViewPort(wide, tall)
			drawSetWindingOrder(GL_CW)
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
			drawClearColor(r, g, b)
			drawClearDepth(1)
			drawSetShader(shaderTest_uniformBlocks)
			drawBindTexture(0, rtTexDisplayTestUhhh)
			drawMesh(TEST_VERTEX_FORMAT, GL_TRIANGLES) { tess -> with(tess) {
				color(Color.WHITE)
				vertex(-0.5, -0.5, 0, 0, 0)
				vertex(+0.5, -0.5, 0, 1, 0)
				vertex(+0.5, +0.5, 0, 1, 1)
				vertex(-0.5, +0.5, 0, 0, 1)
				quad()
			}}
		}

		drawSetSurface(null)
		drawSetWindingOrder(GL_CCW)
		drawSetDepthTestEnable(true)
		drawSetDepthWriteEnable(true)
		drawSetViewPort(winWide, winTall)
		drawClearDepth(1)

		drawGlobalTransform.apply {
			identity()
			perspective(70f, winWide.toFloat() / winTall, 0.001f, 100f)
			scene.applyViewpointTransform(viewMatrix)
			mul(viewMatrix, this)
		}

		scene.prepareObjectsForRendering()
		scene.renderObjects()

		drawBlitSurfaces(
			testSurface, 0, 0, 256, 256,
			null, 32, 32, 128, 128,
			GL_COLOR_BUFFER_BIT,
			GL_LINEAR,
		)

		if (firstFrame)
		{
			window.show()
			firstFrame = false
		}
		window.swapBuffers()
	}

	fun preStep ()
	{
		pevTime = time
		pevMouseCo.set(curMouseCo)
		pevWindowSize.set(windowSize)
		inputVec.set(0.0)
	}

	fun step ()
	{
		if (window.shouldClose)
			throw StopGame()
		time = WindowManager.time
		deltaTime = time - pevTime
		window.getCursorLocation(curMouseCo).sub(pevMouseCo, mouseDelta)
		if (!windowHasFocus)
			mouseGrabbedness(false)

		scene.objects.forEach { it.step?.invoke(scene) }
	}

	override fun close()
	{
	}
}