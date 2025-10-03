package coyote

import coyote.geom.RenderSubmittingTessDigester
import coyote.geom.SavingTessDigester
import coyote.geom.Tesselator
import coyote.geom.VertexFormat
import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Matrix4fStack
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.system.Configuration
import java.awt.Color
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.math.PI
import kotlin.math.cos

const val INITIAL_TITLE = "MACHINE WITNESS"
const val INITIAL_WIDE = 650
const val INITIAL_TALL = 450

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"

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

	glfwSetWindowSizeCallback(windowHandle) { _, w, h ->
		windowSize.set(w, h)
	}

	println(":)")

	glEnable(GL_DEBUG_OUTPUT)
	glDebugMessageCallback(::rendererDebugMessage, 0L)

	val autoQuadShader = SHADERZ[ResourceLocation.of("shader/test auto.lua")]
	val tessTestShader = SHADERZ[ResourceLocation.of("shader/test.lua")]

	val dummy = GPUVertexArray(VertexFormat.NON)

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
	val matrixNIO = matrixSegment.asByteBuffer()

//	val surface = glCreateFramebuffers()
	val tess = Tesselator()
	val submitter = RenderSubmittingTessDigester()

	val testTexture = TEXTUREZ[ResourceLocation.of("texture/screen triangle test.kra")]

	val transform = Matrix4fStack(16)

	val tessSaver = SavingTessDigester()
	val testSavedModel = with(tess) {
		begin(TEST_VERTEX_FORMAT)
		vertexTransform.apply {
			translate(-1.0, -1.0, -1.0)
		}
		textureTransform.apply {
			scale(0.5)
		}
		color(Color.RED)
		vertex(2, 1, 1, 2,1)
		vertex(1, 1, 0, 1,1)
		vertex(1, 2, 1, 1,2)
		triangle()
		color(Color.YELLOW)
		vertex(0, 1, 1, 0,1)
		vertex(1, 1, 0, 1,1)
		vertex(1, 2, 1, 1,2)
		triangle()
		color(Color.BLUE)
		vertex(0, 1, 1, 0,1)
		vertex(1, 1, 0, 1,1)
		vertex(1, 0, 1, 0,0)
		triangle()
		color(Color.WHITE)
		vertex(2, 1, 1, 0,0)
		vertex(1, 1, 0, 1,1)
		vertex(1, 0, 1, 1,0)
		triangle()
		color(Color.RED.darker())
		vertex(2, 1, 1, 0,0)
		vertex(1, 1, 2, 1,1)
		vertex(1, 2, 1, 1,2)
		triangle()
		color(Color.YELLOW.darker())
		vertex(0, 1, 1, 0,0)
		vertex(1, 1, 2, 1,1)
		vertex(1, 2, 1, 1,2)
		triangle()
		color(Color.BLUE.darker())
		vertex(0, 1, 1, 0,0)
		vertex(1, 1, 2, 1,1)
		vertex(1, 0, 1, 1,0)
		triangle()
		color(Color.WHITE.darker())
		vertex(2, 1, 1, 0,0)
		vertex(1, 1, 2, 1,1)
		vertex(1, 0, 1, 1,0)
		triangle()
		end(tessSaver)
	}

	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		glfwPollEvents()
		val time = glfwGetTime()

		val winWide = windowSize.x
		val winTall = windowSize.y
		glViewport(0, 0, winWide, winTall)
//		glClearNamedFramebufferfv(0, GL_COLOR, 0, floatArrayOf(0.5f, 0.1f, 0.4f, 0.0f))
		glClearNamedFramebufferfv(0, GL_DEPTH, 0, floatArrayOf(0.0f))
//		glBlitNamedFramebuffer()

		transform.apply {
			identity()
			perspective(70f, winWide.toFloat()/winTall, 0.001f, 100f)
			rotateX((cos(time * PI) * 45.0).toRadiansf())
			rotateY((time * PI / 2.0).toFloat())
			get(matrixSegment, 0L)
			nglNamedBufferSubData(matrixBuffer, 0L, matrixBufferSize, matrixSegment.address())
		}

		glBindTextureUnit(0, testTexture.handle)
		tessTestShader.bind()
		val uhh = glGetUniformBlockIndex(tessTestShader.vr, "MATRICES")
		glUniformBlockBinding(tessTestShader.vr, uhh, 0)
		glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
		testSavedModel.submit(GL_TRIANGLES)

		glfwSwapBuffers(windowHandle)
		pevWindowSize.set(windowSize)
	}

	WindowManager.close()
}
