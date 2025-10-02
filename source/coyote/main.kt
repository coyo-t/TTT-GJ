package coyote

import coyote.geom.RenderSubmittingTessDigester
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
import java.lang.Math.toRadians
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
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

	glfwShowWindow(windowHandle)
	while (!glfwWindowShouldClose(windowHandle))
	{
		glfwPollEvents()
		val time = glfwGetTime()

		glViewport(0, 0, windowSize.x, windowSize.y)
		glClearNamedFramebufferfv(0, GL_COLOR, 0, floatArrayOf(0.5f, 0.1f, 0.4f, 0.0f))
		glClearNamedFramebufferfv(0, GL_DEPTH, 0, floatArrayOf(0.0f))
//		glBlitNamedFramebuffer()

		glBindTextureUnit(0, testTexture.handle)
		autoQuadShader.bind()
		dummy.bind()
//		glDrawArrays(GL_TRIANGLES, 0, 3)

		transform.apply {
			identity()
//			rotateZ((cos(time * PI) * 0.1).toFloat())
			translate(0f, (cos(time * PI) * 0.1).toFloat(), 0f)

			get(matrixSegment, 0L)
			nglNamedBufferSubData(matrixBuffer, 0L, matrixBufferSize, matrixSegment.address())
		}

		tessTestShader.bind()
		tess.begin(TEST_VERTEX_FORMAT)
		val uhh = glGetUniformBlockIndex(tessTestShader.vr, "MATRICES")
		glUniformBlockBinding(tessTestShader.vr, uhh, 0)
		glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
		tess.vertexTransform.apply {
			translate(0.6, 0.0, 0.0)
			rotateZ(sin(time*PI*0.75)*toRadians(22.5))
			scale(0.5)
			translate(-0.5, -0.5, 0.0)
		}
		tess.color(Color.RED)
		tess.vertex(0, 0, 0, 0,0)
		tess.color(Color.YELLOW)
		tess.vertex(1, 0, 0, 0,0)
		tess.color(Color.BLUE)
		tess.vertex(1, 1, 0, 0,0)
		tess.color(Color.WHITE)
		tess.vertex(0, 1, 0, 0,0)
		tess.quad()
		tess.end(submitter.withMode(GL_TRIANGLES))

		glfwSwapBuffers(windowHandle)
		pevWindowSize.set(windowSize)
	}

	glfwTerminate()
}
