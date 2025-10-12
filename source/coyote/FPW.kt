package coyote

import coyote.resource.ResourceLocation
import coyote.window.Window
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FPW (val window: Window): AutoCloseable
{
	val windowSize = window.getSize(Vector2i())
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
	private val TV_TRANSFORM = Matrix4f().apply {
		translate(0f, 1.16987f, 2.98253f)
		rotateY(180.0.toRadiansf())
	}

	private val viewRotationMatrix = Matrix4f()
	private val viewMatrixTranslated = Matrix4f()
	private val camera3dSwayMatrix = Matrix4f()
	private val camera3dInverseSwayMatrix = Matrix4f()

	fun mouseGrabbedness (gr: Boolean)
	{
		mouseGrabbed = gr
		if (gr)
			window.setCursorMode(GLFW_CURSOR_DISABLED)
		else
			window.setCursorMode(GLFW_CURSOR_NORMAL)
	}



	val model_PlayerSpace = MODELZ.loadModel(ResourceLocation.of("model/player space.lua"))

	val shaderTest_uniformBlocks = SHADERZ[ResourceLocation.of("shader/test.lua")]
	val shaderTest_storedOBJ = SHADERZ[ResourceLocation.of("shader/mesh test.lua")]

	val textureTest_screenTri = TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]
	val testSavedModel = MESHEZ[ResourceLocation.of("mesh/octmeshprev.obj")]
	val model_Crosby = MODELZ.loadModel(ResourceLocation.of("model/crosby.lua"))
	val model_DingusMachine = MODELZ.loadModel(ResourceLocation.of("model/dingus machine.lua"))

	val model_TV = MODELZ.loadModel(ResourceLocation.of("model/tv.lua"))

	val model_TVScreenOff = MODELZ.loadModel(ResourceLocation.of("model/tv screen off.lua"))

	val testRenderTargetTexture = TEXTUREZ.createTexture(256, 256).apply {
		setFilter(GL_LINEAR, GL_LINEAR)
	}
	val testSurface = drawCreateSurface().apply {
		setColorAttachment(testRenderTargetTexture)
		setDepthAttachment(256, 256)
	}

	val rtTexDisplayTestUhhh = TEXTUREZ[ResourceLocation.of("texture/color calibration card.kra")]

	val tex_env_uhh = ResourceLocation.of("texture/env/fpw mk2 labyrinth alpha.png")

	val testFont = FONTZ[ResourceLocation.of("font/kfont2.lua")]

	val osFont = FONTZ[ResourceLocation.of("font/os.lua")]

	val miana_idle_sprites by lazy {
		listOf(
			SPRITEZ.loadSprite(ResourceLocation.of("miana idle towards north")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana idle towards east")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana idle towards south")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana idle towards west")),
		)
	}

	val miana_walk_sprites by lazy {
		listOf(
			SPRITEZ.loadSprite(ResourceLocation.of("miana walk towards north")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana walk towards east")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana walk towards south")),
			SPRITEZ.loadSprite(ResourceLocation.of("miana walk towards west")),
		)
	}

	val miana_joy by lazy {
		SPRITEZ.loadSprite(ResourceLocation.of("miana joy"))
	}

	fun init ()
	{
		//TODO this is really dumb and should be done Auto Magically
		val spritesToLoad = listOf(
			"miana.lua",
		)
		spritesToLoad.forEach {
			SPRITEZ.loadAllSprites(ResourceLocation.of("sprite/$it"))
		}

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

		drawSetFlag(GL_DEBUG_OUTPUT, true)
		drawSetFlag(GL_BLEND, true)
		drawSetDebugMessageCallback(0L, ::rendererDebugMessage)
		drawSetDepthCompareFunc(GL_LESS)
		drawSetCullingSide(GL_BACK)
		drawSetCullingEnabled(true)
	}

	fun pushInviewIdleBob (time:Double, to: Matrix4f)
	{
		var pfac = 0.4
		pfac *= pfac

		val t = time * Math.PI

		val bobx = ((sin(t * .9)  * (.05 + pfac * .5)) + (1.25 * pfac)).toRadians()
		val boby = ((cos(t * .45) * (.1 + pfac * 1.8))).toRadians()

		with (to)
		{
			mapXnZY()
			rotateX(bobx.toFloat())
			rotateY(boby.toFloat())
			rotateZ(-boby.toFloat() * 2)
			mapXZnY()
		}

//		mats.push(matrix_build(0, 0, 0, bobx,boby,-boby * 2., 1,1,1))

	}

	fun draw ()
	{
		val winWide = windowSize.x
		val winTall = windowSize.y
		val time = WindowManager.time
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

		// camera
		drawClearMatrices()
		with (drawProjectionMatrix)
		{
			perspective(70f, windowSize.x.toFloat() / windowSize.y, 0.001f, 100f)
		}

		with (viewRotationMatrix) {
			identity()
			pushInviewIdleBob(time, this)
			rotateX(viewPitch.toRadiansf())
			rotateY(viewYaw.toRadiansf())
		}
		with (viewMatrixTranslated) {
			set(viewRotationMatrix)
			translate(-viewCo.x.toFloat(), -viewCo.y.toFloat(), -viewCo.z.toFloat())
		}
		drawViewMatrix.set(viewRotationMatrix)

		// env
		drawSetDepthWriteEnable(true)
		drawSetDepthTestEnable(false)
		drawSetShader(shaderTest_storedOBJ)
		drawBindTexture(0, TEXTUREZ[tex_env_uhh])
		drawBindTexture(1, testRenderTargetTexture)
		drawSubmit(testSavedModel, GL_TRIANGLES)
		drawClearDepth(1)

		// crosby
		drawSetDepthWriteEnable(true)
		drawSetDepthTestEnable(true)
		drawViewMatrix.set(viewMatrixTranslated)
		drawWorldMatrix.apply {
			identity()
			translate(0f, 0f, 0f)
		}

		model_PlayerSpace.draw()
		drawWorldMatrix.pushMatrix {
			translate(0f, 0.985588f, -3f)
			model_DingusMachine.draw()
		}
		drawWorldMatrix.pushMatrix {
			mul(TV_TRANSFORM)
			model_TV.draw()
			model_TVScreenOff.draw()
		}
		model_Crosby.draw()


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
		with (drawViewMatrix)
		{
			identity()
			val ss = screenScalar.scale.toFloat()
			scale(ss, ss, 1f)
		}

		drawSetBlendMode(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		drawSetShader(shaderTest_uniformBlocks)
		drawWorldMatrix.pushMatrix {
			drawText(
				osFont,
				0, 0,
				0,
				arrayOf(
					"So like this one time i ate a #YWhole Ant!#1",
					"#HI don't know what an ant is #G#_1:3#_0",
					"#_1#1this has 1 #CWhole Unit#1 of spacing now.",
					"The #Ofirst 2 l1nez had no spacing#1. I kind of like it, but it looks bad with punctuation >.>\"",
					"Yes, I added an #R#_3Entire Fucking Command#_1#1 for this :]",
					"also, #Ydingus mcgee#1 over here forgot the [], {}, AND () characters!!",
					"#H(this caused an Infinite Loop -.- )#0",
					"#T1 SW34R TO GOD 3V3RY T1M3 1 4DD ON3 OF MY FUNNY 4SC11 F4C3S 1 F1ND 1 FORGOT 4NOTH3R SYMBOL!!!#1"
				).joinToString("\n")
			)
		}

		val direction = (time).floorToInt() % 4
		drawWorldMatrix.pushMatrix {
			scale(4f, 4f, 1f)
			drawWorldMatrix.pushMatrix {
				translate(64f, 64f, -4f)
				val subimg = (time * 6).floorToInt() % 4
				drawSprite(miana_walk_sprites[direction], subimg, 0, 0)
			}
			drawWorldMatrix.pushMatrix {
				translate(32f, 64f, -5f)
				val subimg = (time * 8).floorToInt() % 5
				drawSprite(miana_joy, subimg, 0, 0)
			}
		}

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