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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi internal val globalTess = Tesselator()
@PublishedApi internal val globalTessSubmitter = RenderSubmittingTessDigester()

private val currentSurfaceHandle get() = currentSurfaceTarget?.handle ?: 0
var currentSurfaceTarget: Surface? = null
	private set
var currentShader: CompiledShaders.ShaderPipeline? = null
	private set
lateinit var currentCapabilities: GLCapabilities
	private set
val drawGlobalTransform = Matrix4fStack(16)
private val matrixBufferSize = ((4*4)*4L)*3
private val matrixSegment = Arena.ofAuto().allocate(matrixBufferSize).apply {
	setAtIndex(JAVA_FLOAT, 0L, 1f)
	setAtIndex(JAVA_FLOAT, 5L, 1f)
	setAtIndex(JAVA_FLOAT, 11L, 1f)
	setAtIndex(JAVA_FLOAT, 15L, 1f)
}

// lazy variable as glCreateBuffers cant be used before context is set
private val matrixBuffer by lazy {
	val l = glCreateBuffers()
	glNamedBufferStorage(l, matrixBufferSize, GL_DYNAMIC_STORAGE_BIT)
	l
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

fun drawCreateCapabilities () = GL.createCapabilities().also { currentCapabilities=it }


fun drawSubmit (vao:Int, pr:Int, indexCount:Int)
{
	drawSubmit(vao, pr, indexCount, GL_UNSIGNED_INT, 0L)
}
fun drawSubmit (vao:Int, pr:Int, indexCount:Int, type:Int, offset:Long=0L)
{
	val currentShader = currentShader ?: return
	drawGlobalTransform.get(matrixSegment, 0L)
	nglNamedBufferSubData(matrixBuffer, 0L, matrixBufferSize, matrixSegment.address())
	val uhh = glGetUniformBlockIndex(currentShader.vr, "MATRICES")
	glUniformBlockBinding(currentShader.vr, uhh, 0)
	glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixBuffer)
	glBindVertexArray(vao)
	glDrawElements(pr, indexCount, type, offset)
}

fun drawSubmit (tess: TesselatorStore, pr:Int)
{
	if (tess.vertexCount <= 0)
		return
	drawSubmit(tess.getVAO(), pr, tess.indexCount)
}

fun drawSetState (what:Int, to: Boolean)
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
	drawSetState(GL_DEPTH_TEST, to)
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