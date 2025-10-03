package coyote

import coyote.geom.RenderSubmittingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Math.lerp
import org.joml.Matrix4fStack
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
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
			glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
		else
			glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
	}

	glfwSetKeyCallback(windowHandle) { _, key, scancode, action, mods ->
		if (action == GLFW_PRESS)
			inputMap[key] = true
		else if (action == GLFW_RELEASE)
			inputMap[key] = false
		when (key)
		{
			GLFW_KEY_ESCAPE if (action == GLFW_PRESS) -> mouseGrabbedness(!mouseGrabbed)
		}
	}

	var currentShader: CompiledShaders.ShaderPipeline? = null
	fun drawSubmit (tess: TesselatorStore, pr:Int)
	{
		if (tess.vertexCount <= 0)
			return
		val currentShader = currentShader ?: return
		transform.get(matrixSegment, 0L)
		nglNamedBufferSubData(matrixBuffer, 0L, matrixBufferSize, matrixSegment.address())
		val uhh = glGetUniformBlockIndex(currentShader.vr, "MATRICES")
		glUniformBlockBinding(currentShader.vr, uhh, 0)
		glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
		tess.submit(pr)
	}
	fun drawUseShader (res: CompiledShaders.ShaderPipeline)
	{
		if (res == currentShader)
			return
		res.bind()
		currentShader = res
	}

	var currentSurfaceTarget = 0

	fun drawSetSurface (who: Int)
	{
		if (currentSurfaceTarget == who)
			return
		currentSurfaceTarget = who
		glBindFramebuffer(GL_FRAMEBUFFER, who)
	}
	fun drawResetSurface ()
	{
		drawSetSurface(0)
	}
	fun drawSetViewPort (wide:Number, tall:Number)
	{
		glViewport(0, 0, wide.toInt(), tall.toInt())
	}
	fun drawBindTexture (slot:Int, who: Texture)
	{
		glBindTextureUnit(slot, who.handle)
	}
	fun drawClearColor (r:Number, g:Number, b:Number, a:Number=1)
	{
		glClearNamedFramebufferfv(currentSurfaceTarget, GL_COLOR, 0, floatArrayOf(
			r.toFloat(),
			g.toFloat(),
			b.toFloat(),
			a.toFloat(),
		))
	}
	fun drawClearColor (c: Color)
	{
		drawClearColor(c.red/255.0, c.green/255.0, c.blue/255.0, c.alpha/255.0)
	}

	fun drawClearDepth (d: Number)
	{
		glClearNamedFramebufferfv(currentSurfaceTarget, GL_DEPTH, 0, floatArrayOf(d.toFloat()))
	}

	val testRenderTargetTexture = TEXTUREZ.createTexture(256, 256).apply {
		setFilter(GL_LINEAR, GL_LINEAR)
	}
	val testFBDepth = glCreateRenderbuffers()
	glNamedRenderbufferStorage(testFBDepth, GL_DEPTH_COMPONENT, 256, 256)
	val testFrameBuffer = glCreateFramebuffers()
	glNamedFramebufferTexture(testFrameBuffer, GL_COLOR_ATTACHMENT0, testRenderTargetTexture.handle, 0)
	glNamedFramebufferRenderbuffer(testFrameBuffer, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, testFBDepth)

	val tessSubmitter = RenderSubmittingTessDigester()
	val tess = Tesselator()

	val rtTexDisplayTestUhhh = TEXTUREZ[ResourceLocation.of("texture/color calibration card.kra")]

	glDepthFunc(GL_LESS)

	var time = WindowManager.time
	var pevTime = time
	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		pevTime = time
		val time = WindowManager.time
		val deltaTime = time - pevTime
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
		drawSetViewPort(winWide, winTall)
		run {
			val (wide,tall) = testRenderTargetTexture.size
			drawSetSurface(testFrameBuffer)
			drawClearColor(Color.WHITE)
			drawSetSurface(0)
			drawSetViewPort(wide, tall)
			glEnable(GL_DEPTH_TEST)
			glDepthMask(true)
			transform.apply {
				identity()
//				ortho(0f, wide.toFloat(), tall.toFloat(), 0f, 0f, 10f)
			}
			val r = lerp(sin(time * 0.9 * PI) * 0.5 + 0.5, 0.2, 0.8)
			val g = lerp(cos(time * 0.8 * PI) * 0.5 + 0.5, 0.3, 0.9)
			val b = 0.7
			drawClearColor(r,g,b)
			drawClearDepth(1)
			with (tess)
			{
				begin(TEST_VERTEX_FORMAT)
				color(Color.WHITE)
				vertexTransform.apply {
					identity()
//					rotateZ(sin(time * PI) * 45.0.toRadians())
//					scale(100.0)
//					translate(-1.0, -1.0, 0.0)
//					scale(2.0)
				}
				val zp = sin(time * PI)
				vertex(-0.5,-0.5,zp, 0,0)
				vertex(+0.5,-0.5,zp, 1,0)
				vertex(+0.5,+0.5,zp, 1,1)
				vertex(-0.5,+0.5,zp, 0,1)
				quad()
				drawUseShader(shaderTest_uniformBlocks)
				drawBindTexture(0, rtTexDisplayTestUhhh)
				end(tessSubmitter.withMode(GL_TRIANGLES))
			}
			drawResetSurface()
		}

		drawSetSurface(0)
		glEnable(GL_DEPTH_TEST)
		glDepthMask(true)
		drawSetViewPort(winWide, winTall)
		drawClearDepth(1)
		transform.apply {
			identity()
			perspective(70f, winWide.toFloat()/winTall, 0.001f, 100f)
			rotateX(viewPitch.toRadiansf())
			rotateY(viewYaw.toRadiansf())
			translate(-viewCo.x.toFloat(), -viewCo.y.toFloat(), -viewCo.z.toFloat())
		}

		drawUseShader(shaderTest_storedOBJ)
		drawBindTexture(0, textureTest_labyrinthOctoEnv)
		drawBindTexture(1, testRenderTargetTexture)
		drawSubmit(testSavedModel, GL_TRIANGLES)

		drawUseShader(shaderTest_uniformBlocks)
		drawBindTexture(0, TEXTUREZ[TEXTURE_WHITE])
		drawSubmit(modelTest_compass, GL_LINES)

		glBlitNamedFramebuffer(testFrameBuffer, 0, 0, 0, 256, 256, 32, 32, 128, 128, GL_COLOR_BUFFER_BIT, GL_NEAREST)

		glfwSwapBuffers(windowHandle)
		pevWindowSize.set(windowSize)
	}

	WindowManager.close()
}
