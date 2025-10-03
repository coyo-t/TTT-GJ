package coyote

import coyote.geom.VertexFormat
import coyote.geom.VertexFormatBuilder
import org.joml.Math.clamp
import org.joml.Matrix4fc
import org.joml.Vector3d
import org.lwjgl.opengl.GL45C.*
import org.lwjgl.opengl.GLDebugMessageCallback
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.math.floor


fun Double.floorToInt () = floor(this).toInt()
fun Double.floorToInt (min:Int, max:Int) = clamp(floorToInt(), min, max)

fun Vector3d.set (x:Number, y:Number, z:Number): Vector3d
	= set(x.toDouble(), y.toDouble(), z.toDouble())

@OptIn(ExperimentalContracts::class)
inline fun buildVertexFormat (cb: VertexFormatBuilder.()->Unit): VertexFormat
{
	contract { callsInPlace(cb, InvocationKind.EXACTLY_ONCE) }
	return VertexFormatBuilder().apply(cb).build()
}

fun glTypeByteSize (type: Int): Long
{
	return when (type)
	{
		GL_BYTE, GL_UNSIGNED_BYTE -> 1
		GL_SHORT, GL_UNSIGNED_SHORT -> 2
		GL_INT, GL_UNSIGNED_INT -> 4
		GL_FLOAT -> 4
		else -> throw IllegalArgumentException()
	}
}

fun rendererDebugMessage (source:Int, type:Int, id:Int, severity:Int, mLen:Int, mPtr:Long, ud:Long)
{
	val sSevere = when (severity) {
		GL_DEBUG_SEVERITY_NOTIFICATION -> {
			// "NOTIFICATION"
			return
		}
		GL_DEBUG_SEVERITY_LOW -> "LOW"
		GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
		GL_DEBUG_SEVERITY_HIGH -> "HIGH"
		else -> "??? $severity"
	}

	val sSrc = when (source) {
		GL_DEBUG_SOURCE_API -> "API"
		GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW SYSTEM"
		GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER COMPILER"
		GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD PARTY"
		GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
		GL_DEBUG_SOURCE_OTHER -> "OTHER"
		else -> "??? $source"
	}
	val sType = when (type) {
		GL_DEBUG_TYPE_ERROR -> "ERROR"
		GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR"
		GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR"
		GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
		GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
		GL_DEBUG_TYPE_MARKER -> "MARKER"
		GL_DEBUG_TYPE_OTHER -> "OTHER"
		else -> "??? $type"
	}
	val sMessage = GLDebugMessageCallback.getMessage(mLen, mPtr)
	val fin = "$sSrc, $sType, $sSevere, $id, $sMessage"
	if (severity == GL_DEBUG_SEVERITY_HIGH && type == GL_DEBUG_TYPE_ERROR)
	{
		throw IllegalStateException(fin)
	}
	println(fin)
}

fun createBuffer (size:Long): ByteBuffer
{
	return ByteBuffer.allocateDirect(size.toInt()).order(ByteOrder.nativeOrder())
}

fun loadSystemLibrary (p: Path)
{
	try
	{
		System.loadLibrary(p.normalize().invariantSeparatorsPathString)
	}
	catch (_: UnsatisfiedLinkError)
	{
		System.load(p.normalize().toAbsolutePath().invariantSeparatorsPathString)
	}
}

fun loadSystemLibrary (s: String)
{
	try
	{
		System.loadLibrary(s)
	}
	catch (_: UnsatisfiedLinkError)
	{
		System.load(s)
	}
}

fun applyVertexFormat (format: VertexFormat, vaoHandle: Int)
{
	for (attribute in format.attributes)
	{
		val i = attribute.index
		glEnableVertexArrayAttrib(vaoHandle, i)
		glVertexArrayAttribBinding(vaoHandle, i, 0)
		glVertexArrayAttribFormat(
			vaoHandle,
			i,
			attribute.elementCount,
			attribute.type,
			attribute.normalized,
			attribute.byteOffset,
		)
	}
}

fun Double.toRadians () = Math.toRadians(this)
fun Double.toRadiansf () = this.toRadians().toFloat()

fun Matrix4fc.get (into: MemorySegment, offset: Long)
{
	into[JAVA_FLOAT, offset] = m00()
	into[JAVA_FLOAT, offset+(4*1L)] = m01()
	into[JAVA_FLOAT, offset+(4*2L)] = m02()
	into[JAVA_FLOAT, offset+(4*3L)] = m03()
	into[JAVA_FLOAT, offset+(4*4L)] = m10()
	into[JAVA_FLOAT, offset+(4*5L)] = m11()
	into[JAVA_FLOAT, offset+(4*6L)] = m12()
	into[JAVA_FLOAT, offset+(4*7L)] = m13()
	into[JAVA_FLOAT, offset+(4*8L)] = m20()
	into[JAVA_FLOAT, offset+(4*9L)] = m21()
	into[JAVA_FLOAT, offset+(4*10L)] = m22()
	into[JAVA_FLOAT, offset+(4*11L)] = m23()
	into[JAVA_FLOAT, offset+(4*12L)] = m30()
	into[JAVA_FLOAT, offset+(4*13L)] = m31()
	into[JAVA_FLOAT, offset+(4*14L)] = m32()
	into[JAVA_FLOAT, offset+(4*15L)] = m33()
}
