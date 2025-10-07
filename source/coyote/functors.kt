package coyote

import coyote.geom.SavingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.geom.VertexFormat
import coyote.geom.VertexFormatBuilder
import coyote.resource.ResourceLocation
import org.joml.Matrix4fc
import org.joml.Vector3d
import org.lwjgl.opengl.GL45C.*
import org.lwjgl.opengl.GLDebugMessageCallback
import party.iroiro.luajava.value.LuaTableValue
import java.io.InputStreamReader
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
fun Double.floorToInt (min:Int, max:Int) = Math.clamp(floorToInt().toLong(), min, max)
fun Double.clamped (doubleRange: ClosedRange<Double>)
	= Math.clamp(this, doubleRange.start, doubleRange.endInclusive)
fun Double.clampedSym (radius:Double)
	= Math.clamp(this, -radius, +radius)

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

fun createFont (fontName: ResourceLocation): Font
{
	LuaCoyote().use { L ->
		L.openLibraries()
		val maybe = InputStreamReader(RESOURCES[fontName]!!.openInputStream()).use { stream ->
			L.run(stream.readText())
			L.get() as LuaTableValue
		}
		val picName = ResourceLocation.of(maybe["source"].toString())
		val pic = TEXTUREZ.imageManager[picName]
		L.push(maybe)
		L.getField(-1, "margin")
		val margin = if (L.isNumber(-1))
			L.toNumber(-1)
		else
			0.0
		L.pop(2)
		val rcpWide = 1.0 / pic.wide
		val rcpTall = 1.0 / pic.tall
		val xMargin = margin * rcpWide
		val yMargin = margin * rcpTall
		val keyColor = pic[0, 0]
		val charSet = maybe["chars"].toString()
		val charCount = charSet.length
		var mode = false
		var i = 0
		var j = 0
		val stop = pic.wide
		var currentChar = 0
		val glf = buildList {
			while (i < (stop+1) && currentChar < charCount)
			{
				val atEnd = i == stop
				val pixel = pic[i, 0]
				val whatStage = mode || atEnd
				if (whatStage)
				{
					if (atEnd || pixel == keyColor)
					{
						val uv0 = j * rcpWide
						val uv1 = i * rcpWide
						this += Font.Glyph(
							char = charSet[currentChar],
							patch = Rectangle(uv0+xMargin, yMargin, uv1-xMargin, 1.0-yMargin),
							advance = i-j,
							height = pic.tall,
						)
						currentChar += 1
						mode = false
					}
				}
				else
				{
					if (pixel != keyColor)
					{
						j = i
						mode = true
					}
				}

				i += 1
			}
		}
		return Font(TEXTUREZ.add(fontName, pic), glf, pic.tall).also { pic.close() }
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

@OptIn(ExperimentalContracts::class)
inline fun buildModel (format: VertexFormat, cb: Tesselator.()->Unit): TesselatorStore
{
	contract { callsInPlace(cb, InvocationKind.EXACTLY_ONCE) }
	val tess = Tesselator()
	tess.begin(format)
	tess.cb()
	return tess.end(SavingTessDigester())
}
