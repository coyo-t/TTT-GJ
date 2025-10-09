package coyote

import coyote.resource.ResourceLocation
import coyote.window.WindowHint
import coyote.window.WindowManager
import org.joml.Math.lerp
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL46C.GL_DEBUG_OUTPUT
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FPW: AutoCloseable
{
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
	var screenScale = 3
	val screenScalar = ScreenScalar(320, 240).apply {
		sizeProvider = { windowSize }
		preferredScaleProvider = { screenScale }
	}

	private var firstFrame = true

	val window = with(WindowManager) {
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
		createWindow("MACHINE WITNESS", windowSize).also {
			it.setSizeLimits(
				minSize = 320 to 240,
				maxSize = null,
			)
		}
	}

	fun mouseGrabbedness (gr: Boolean)
	{
		mouseGrabbed = gr
		if (gr)
			window.setCursorMode(GLFW_CURSOR_DISABLED)
		else
			window.setCursorMode(GLFW_CURSOR_NORMAL)
	}

	val modsz = ModelManager(RESOURCES)

	val testMommy by lazy {
		modsz.loadWavefront(RESOURCES[ResourceLocation.of("mesh/player space.obj")]!!)
	}

	val shaderTest_uniformBlocks by lazy {
		SHADERZ[ResourceLocation.of("shader/test.lua")]
	}
	val shaderTest_storedOBJ by lazy {
		SHADERZ[ResourceLocation.of("shader/mesh test.lua")]
	}
	val shader_Crosby by lazy {
		SHADERZ[ResourceLocation.of("shader/crosby.lua")]
	}

	val textureTest_screenTri by lazy {
		TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]
	}
	val testSavedModel by lazy {
		MESHEZ[ResourceLocation.of("mesh/octmeshprev.obj")]
	}
	val model_Crosby by lazy {
		MESHEZ[ResourceLocation.of("mesh/crosby.obj")]
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

	val texture_Untextured by lazy {
		TEXTUREZ[ResourceLocation.of("texture/untextured.png")]
	}

	val tex_env_uhh = ResourceLocation.of("texture/env/fpw mk2 labyrinth alpha.png")

	val testFont by lazy {
		FONTZ[ResourceLocation.of("font/kfont2.lua")]
	}

	fun init ()
	{
		window.setRawMouseMotion(true)
		glfwSetWindowSizeCallback(window.handle) { _, w, h ->
			windowSize.set(w, h)
			screenScalar.update()
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
		drawInitialize()
		drawSetFlag(GL_DEBUG_OUTPUT, true)
		drawSetFlag(GL_BLEND, true)
		drawSetDebugMessageCallback(0L, ::rendererDebugMessage)
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
			drawClearMatrices()
			drawSetViewPort(wide, tall)
			drawSetWindingOrder(GL_CW)
			drawSetCullingEnabled(true)
			drawSetDepthTestEnable(true)
			drawSetDepthWriteEnable(true)
			drawProjectionMatrix.apply {
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

		//#region scene
		drawSetSurface(null)
		drawSetWindingOrder(GL_CCW)
		drawSetCullingEnabled(true)
		drawSetDepthTestEnable(true)
		drawSetDepthWriteEnable(true)
		drawSetViewPort(winWide, winTall)
		drawClearDepth(1)

		// camera
		drawClearMatrices()
		with (drawProjectionMatrix)
		{
			perspective(70f, windowSize.x.toFloat() / windowSize.y, 0.001f, 100f)
		}
		with (drawViewMatrix)
		{
			rotateX(viewPitch.toRadiansf())
			rotateY(viewYaw.toRadiansf())
			translate(-viewCo.x.toFloat(), -viewCo.y.toFloat(), -viewCo.z.toFloat())
		}

		// env
		drawSetShader(shaderTest_storedOBJ)
		drawBindTexture(0, TEXTUREZ[tex_env_uhh])
		drawBindTexture(1, testRenderTargetTexture)
		drawSubmit(testSavedModel, GL_TRIANGLES)

		// crosby
		drawWorldMatrix.apply {
			identity()
			translate(0f, 0f, 0f)
		}
		drawBindTexture(0, texture_Untextured)
		drawSetShader(shader_Crosby)
		drawSubmit(model_Crosby, GL_TRIANGLES)

		drawBindTexture(0, TEXTUREZ[ResourceLocation.of("texture/the pod people trumpy.kra")])
		for (it in testMommy)
		{
			drawSubmit(it, GL_TRIANGLES)
		}

		//#endregion

		//#region gui

		drawClearDepth(1)
		drawClearMatrices()
		drawSetCullingEnabled(false)
		with (drawProjectionMatrix)
		{
			identity()
			ortho(0f, windowSize.x.toFloat(), windowSize.y.toFloat(), 0f, 0f, 10f)
		}

		drawSetBlendMode(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		drawSetShader(shaderTest_uniformBlocks)
		drawBindTexture(0, testFont.texture)

		drawWorldMatrix.pushMatrix().scale(screenScalar.scale.toFloat())
		drawText(
			testFont,
			0, 0,
			0,
			arrayOf(
				"So like this one time i ate a #YWhole Ant!#1",
				"#HI don't know what an ant is #G:3",
			).joinToString("\n")
		)
		drawWorldMatrix.popMatrix()

		//#endregion

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
		screenScalar.update()
		window.getCursorLocation(curMouseCo).sub(pevMouseCo, mouseDelta)
		if (!windowHasFocus)
			mouseGrabbedness(false)

		//#region step camera
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
		//#endregion
	}

	override fun close()
	{
	}
}