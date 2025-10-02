package coyote.geom

import coyote.geom.VertexFormat
import coyote.floorToInt
import coyote.set
import org.joml.Matrix3x2dStack
import org.joml.Matrix4x3dStack
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d
import java.awt.Color
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class Tesselator
{
	private var tesselating = false

	private val indices = mutableListOf<Int>()

	private var arena = Arena.ofConfined()
	private var vertexMemory = arena.allocate(0x11111)
	private var vertexCursor = 0L
	private var logicalVertexCount = 0

	private fun ensure (amount:Number)
	{
		val amount=amount.toLong()
		val wish = vertexCursor + amount
		val currentSize = vertexMemory.byteSize()
		if (wish <= currentSize)
			return
		val newSize = wish + (wish ushr 1)
		val newArena = Arena.ofConfined()
		val newMemory = newArena.allocate(newSize).copyFrom(vertexMemory)
		arena.close()
		arena = newArena
		vertexMemory = newMemory
	}
	private fun currentVertexAddress (): MemorySegment
	{
		return vertexMemory.asSlice(vertexCursor, currentVertexFormat.byteSize.toLong())
	}

	val vertexTransform = Matrix4x3dStack(32)
	val textureTransform = Matrix3x2dStack(16)

	private var pLocation: VertexFormat.Attribute? = null
	private var pUV0: VertexFormat.Attribute? = null
	private var pUV1: VertexFormat.Attribute? = null
	private var pNormal: VertexFormat.Attribute? = null
	private var pColor: VertexFormat.Attribute? = null

	private val currentColor = Vector4d()
	private val currentNormal = Vector3d()
	private val currentUV0 = Vector2d()
	private val currentUV1 = Vector2d()

	private val tempv4 = Vector4d()
	private val tempv3 = Vector3d()
	private val tempv2 = Vector2d()

	lateinit var currentVertexFormat: VertexFormat
		private set

	private fun checkState (shouldBe: Boolean)
	{
		check(tesselating == shouldBe) {
			if (shouldBe)
				"not building"
			else
				"already building"
		}
	}
	private fun checkAndChangeState (to: Boolean)
	{
		checkState(!to)
		tesselating = to
	}

	private fun clear ()
	{
		vertexCursor = 0
		logicalVertexCount = 0
		indices.clear()
		pLocation = null
		pUV0 = null
		pUV1 = null
		pNormal = null
		pColor = null
		currentColor.set(1.0, 1.0, 1.0, 1.0)
		vertexTransform.clear()
		textureTransform.clear()
	}

	fun begin (format: VertexFormat)
	{
		checkAndChangeState(true)
		clear()
		currentVertexFormat = format

		pLocation = format.byUsage["location"]
		pUV0 = format.byUsage["uv0"]
		pUV1 = format.byUsage["uv1"]
		pNormal = format.byUsage["normal"]
		pColor = format.byUsage["color"]
	}

	fun quad ()
	{
		val lv = logicalVertexCount - 4
		indices += lv
		indices += lv+1
		indices += lv+2
		indices += lv
		indices += lv+2
		indices += lv+3
	}

	fun triangle ()
	{
		val lv = logicalVertexCount - 3
		indices += lv
		indices += lv+1
		indices += lv+2
	}

	fun vertex (x:Number, y:Number, z:Number, u:Number, v:Number)
	{
		texture(u,v)
		vertex(x,y,z)
	}

	fun vertex (x:Number, y:Number, z:Number)
	{
		ensure(currentVertexFormat.byteSize)

		//TODO these assume the types of the attributes, but the
		// attribute should handle writing to the address -.-
		val pWrite = currentVertexAddress()
		pNormal?.let {
			val ofs = it.byteOffset.toLong()
			val v = vertexTransform.transformDirection(tempv3.set(currentNormal))
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs] = v.x.toFloat()
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs+4] = v.y.toFloat()
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs+8] = v.z.toFloat()
		}
		pUV0?.let {
			val ofs = it.byteOffset.toLong()
			val v = textureTransform.transformPosition(tempv2.set(currentUV0))
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs] = v.x.toFloat()
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs+4] = v.y.toFloat()
		}
		pColor?.let {
			val ofs = it.byteOffset.toLong()
			val v = currentColor
			pWrite[ValueLayout.JAVA_BYTE, ofs] = (v.x * 255.0).floorToInt(0, 255).toByte()
			pWrite[ValueLayout.JAVA_BYTE, ofs+1] = (v.y * 255.0).floorToInt(0, 255).toByte()
			pWrite[ValueLayout.JAVA_BYTE, ofs+2] = (v.z * 255.0).floorToInt(0, 255).toByte()
			pWrite[ValueLayout.JAVA_BYTE, ofs+3] = (v.w * 255.0).floorToInt(0, 255).toByte()
		}
		pLocation?.let {
			val ofs = it.byteOffset.toLong()
			val v = vertexTransform.transformPosition(tempv3.set(x, y, z))
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs] = v.x.toFloat()
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs+4] = v.y.toFloat()
			pWrite[ValueLayout.JAVA_FLOAT_UNALIGNED, ofs+8] = v.z.toFloat()
		}
		logicalVertexCount += 1
		vertexCursor += currentVertexFormat.byteSize
	}

	fun texture (u:Number, v:Number)
	{
		currentUV0.set(u.toDouble(), v.toDouble())
	}

	fun normal (x:Number, y:Number, z:Number)
	{
		currentNormal.set(x.toDouble(), y.toDouble(), z.toDouble())
	}

	fun color (r:Number, g:Number, b:Number, a:Number=1)
	{
		currentColor.set(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
	}

	fun color (c: Color)
	{
		color(c.red/255.0, c.green/255.0, c.blue/255.0, c.alpha/255.0)
	}

	fun end (guts: TesselatorDigester)
	{
		checkAndChangeState(false)

		guts.digest(
			currentVertexFormat,
			vertexMemory.asSlice(0L, vertexCursor),
			logicalVertexCount,
			indices,
		)
	}


//	val vbo: Int
//	val ibo: Int
//	val vao: Int
//
//	init
//	{
//		vbo = glCreateBuffers()
//		ibo = glCreateBuffers()
//		vao = glCreateVertexArrays()
//	}

}