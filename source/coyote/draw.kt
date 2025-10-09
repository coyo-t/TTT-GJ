package coyote

import coyote.geom.RenderSubmittingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.geom.VertexFormat
import coyote.ren.CompiledShaders
import org.joml.Matrix4fStack
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.GLDebugMessageCallbackI
import java.awt.Color
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.util.Optional
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrNull

@PublishedApi internal val globalTess = Tesselator()
@PublishedApi internal val globalTessSubmitter = RenderSubmittingTessDigester()

val TEST_VERTEX_FORMAT = buildVertexFormat {
	location3D()
	textureCoord()
	byteColor()
}

var isInitialized = false
	private set
private val currentSurfaceHandle get() = currentSurfaceTarget?.handle ?: 0
var currentSurfaceTarget: Surface? = null
	private set
var currentShader: CompiledShaders.ShaderPipeline? = null
	private set
lateinit var currentCapabilities: GLCapabilities
	private set

val drawWorldMatrix = Matrix4fStack(32)
val drawViewMatrix = Matrix4fStack(16)
val drawProjectionMatrix = Matrix4fStack(8)
private val MATRIX_SIZE = ((4*4)*Float.SIZE_BYTES).toLong()
private val MATRIX_BUFFER_SIZE = MATRIX_SIZE*3L
private val matrixSegment = Arena.ofAuto().allocate(MATRIX_BUFFER_SIZE).apply {
	setAtIndex(JAVA_FLOAT, 0L, 1f)
	setAtIndex(JAVA_FLOAT, 5L, 1f)
	setAtIndex(JAVA_FLOAT, 11L, 1f)
	setAtIndex(JAVA_FLOAT, 15L, 1f)
}
private var shaderMatrixHandleTable = mutableMapOf<Int, Optional<Int>>()

// lazy variable as glCreateBuffers cant be used before context is set
private val matrixBuffer by lazy {
	val l = glCreateBuffers()
	glNamedBufferStorage(l, MATRIX_BUFFER_SIZE, GL_DYNAMIC_STORAGE_BIT)
	l
}

fun drawClearMatrices ()
{
	drawWorldMatrix.clear()
	drawViewMatrix.clear()
	drawProjectionMatrix.clear()
}


@OptIn(ExperimentalContracts::class)
inline fun <T> drawMesh (format: VertexFormat, pr:Int, block:(Tesselator)->T): T
{
	contract { callsInPlace(block,  InvocationKind.EXACTLY_ONCE) }
	globalTess.begin(format)
	val outs = block(globalTess)
	globalTess.end(globalTessSubmitter.withMode(pr))
	return outs
}

fun drawText (font: Font, spacing: Number, x: Number, y: Number, string: String)
{
	val lh = font.lineHeight
	var xText1 = 0
	var yText1 = 0
	var spacing = spacing.toInt()
	val CTL = '#'
	drawBindTexture(0, font.texture)
	drawMesh(TEST_VERTEX_FORMAT, GL_TRIANGLES) { tess ->
		tess.vertexTransform.translate(x.toDouble(), y.toDouble(), 0.0)
		var i = 0
		while (i < string.length)
		{
			val ch = string[i]
			if (ch == CTL)
			{
				var skip = true
				i += 1
				when (string[i])
				{
					CTL -> { skip = false }
					'R' -> tess.color(Color.RED)
					'G' -> tess.color(Color.GREEN)
					'B' -> tess.color(Color.BLUE)
					'Y' -> tess.color(Color.YELLOW)
					'C' -> tess.color(Color.CYAN)
					'T' -> tess.color(Color.CYAN.darker())
					'M' -> tess.color(Color.MAGENTA)
					'O' -> tess.color(Color.ORANGE)
					'H' -> tess.color(Color.GRAY)
					'1', 'W' -> tess.color(Color.WHITE)
					'0' -> tess.color(Color.BLACK)
					'_' -> {
						i += 1
						val news = string[i].digitToIntOrNull() ?: 0
						spacing = news
					}
				}
				if (skip)
				{
					i += 1
					continue
				}
			}

			if (ch == '\n')
			{
				xText1 = 0
				yText1 += lh
				i += 1
				continue
			}
			val charIndex = font[ch]
			if (charIndex == null)
			{
				i += 1
				continue
			}
			val chAdvance = charIndex.advance
			if (ch == ' ')
			{
				xText1 += chAdvance
				i += 1
				continue
			}

			val chRect = charIndex.patch
			val fontHeight = charIndex.height
			tess.vertex(xText1, yText1 + fontHeight, 0, chRect.x0, chRect.y1)
			tess.vertex(xText1 + chAdvance, yText1 + fontHeight, 0, chRect.x1, chRect.y1)
			tess.vertex(xText1 + chAdvance, yText1, 0, chRect.x1, chRect.y0)
			tess.vertex(xText1, yText1, 0, chRect.x0, chRect.y0)
			tess.quad()
			xText1 += chAdvance + spacing
			i += 1
		}
	}
}

fun drawBlitSurfaces (
	src:Surface?,
	srcX0: Int, srcY0: Int,
	srcX1: Int, srcY1: Int,
	dst:Surface?,
	dstX0: Int, dstY0: Int,
	dstX1: Int, dstY1: Int,
	mask: Int,
	filter: Int,
)
{
	glBlitNamedFramebuffer(
		src?.handle ?: 0,
		dst?.handle ?: 0,
		srcX0, srcY0, srcX1, srcY1,
		dstX0, dstY0, dstX1, dstY1,
		mask,
		filter,
	)

}

fun drawCreateSurface (): Surface
{
	val b = glCreateFramebuffers()
	return Surface(b)
}

fun drawCreateSurface (w:Int, h:Int): Surface
{
	check(w >= 1 && h >= 1) { "$w, $h" }
	val surface = drawCreateSurface()
	TODO()
}

fun drawInitialize ()
{
	currentCapabilities = GL.createCapabilities()
	isInitialized = true
}

fun drawSetBlendMode (src:Int, dst:Int)
{
	glBlendFunc(src, dst)
}



fun drawSubmit (vao:Int, pr:Int, indexCount:Int)
{
	drawSubmit(vao, pr, indexCount, GL_UNSIGNED_INT, 0L)
}
fun drawSubmit (vao:Int, pr:Int, indexCount:Int, type:Int, offset:Long=0L)
{
	val currentShader = currentShader ?: return
	drawProjectionMatrix.get(matrixSegment, 0L)
	drawViewMatrix.get(matrixSegment, MATRIX_SIZE)
	drawWorldMatrix.get(matrixSegment, MATRIX_SIZE*2)

	nglNamedBufferSubData(matrixBuffer, 0L, MATRIX_BUFFER_SIZE, matrixSegment.address())
	shaderMatrixHandleTable.getOrPut(currentShader.vr) {
		val m = glGetUniformBlockIndex(currentShader.vr, "MATRICES")
		if (m < 0)
			Optional.empty()
		else
			Optional.of(m)
	}.getOrNull()?.let { uhh ->
		glUniformBlockBinding(currentShader.vr, uhh, 0)
		glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
	}
	glBindVertexArray(vao)
	glDrawElements(pr, indexCount, type, offset)
}

fun drawSubmit (tess: TesselatorStore, pr:Int)
{
	if (tess.vertexCount <= 0)
		return
	drawSubmit(tess.getVAO(), pr, tess.indexCount)
}

fun drawSetFlag (what:Int, to: Boolean)
{
	if (to)
		glEnable(what)
	else
		glDisable(what)
}
fun drawSetDebugMessageCallback (userData:Long, cb: GLDebugMessageCallbackI)
{
	glDebugMessageCallback(cb, userData)
}
fun drawSetDepthCompareFunc (to: Int)
{
	glDepthFunc(to)
}
fun drawSetDepthTestEnable (to: Boolean)
{
	drawSetFlag(GL_DEPTH_TEST, to)
}
fun drawSetDepthWriteEnable (to: Boolean)
{
	glDepthMask(to)
}

fun drawSetShader (res: CompiledShaders.ShaderPipeline)
{
	if (res == currentShader)
		return
	res.bind()
	currentShader = res
}
fun drawSetSurface (who: Surface?)
{
	if (currentSurfaceTarget == who)
		return
	currentSurfaceTarget = who
	glBindFramebuffer(GL_FRAMEBUFFER, who?.handle ?: 0)
}
@OptIn(ExperimentalContracts::class)
inline fun <T> drawToSurface (who:Surface?, cb:()->T):T
{
	contract { callsInPlace(cb, InvocationKind.EXACTLY_ONCE) }
	val pev = currentSurfaceTarget
	drawSetSurface(who)
	return cb().also { drawSetSurface(pev) }
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
	glClearNamedFramebufferfv(currentSurfaceHandle, GL_COLOR, 0, floatArrayOf(
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
	glClearNamedFramebufferfv(currentSurfaceHandle, GL_DEPTH, 0, floatArrayOf(d.toFloat()))
}

fun drawSetCullingEnabled (d: Boolean)
{
	drawSetFlag(GL_CULL_FACE, d)
}

fun drawSetCullingSide (w:Int)
{
	glCullFace(w)
}

fun drawSetWindingOrder (w:Int)
{
	glFrontFace(w)
}

