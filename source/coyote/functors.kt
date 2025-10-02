package coyote

import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.opengl.GL45C.*
import org.lwjgl.opengl.GLDebugMessageCallback
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.invariantSeparatorsPathString


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

fun getWindowManagerError () = stackPush().use { stack ->
	val name = stack.mallocPointer(1)
	val errc = glfwGetError(name)
	name.stringASCII to errc
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
	println("$sSrc, $sType, $sSevere, $id, $sMessage")
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
