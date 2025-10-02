package coyote.geom

import coyote.glTypeByteSize
import org.lwjgl.opengl.GL11C

class VertexFormatBuilder
{

	private val types = mutableListOf<Int>()
	private val byteOffsets = mutableListOf<Int>()
	private val elementCounts = mutableListOf<Int>()
	private val signedness = mutableListOf<Boolean>()
	private val normalizedness = mutableListOf<Boolean>()
	private val locations = mutableListOf<Int>()
	private val usages = mutableMapOf<String, Int>()

	private var stride = 0
	private var locationCounter = 0

	fun custom (type:Int, elems:Int, signed:Boolean, normalized:Boolean, usage:String?=null)
	{
		require(elems in 1..4) { "element count $elems out of range" }
		val sizeof = glTypeByteSize(type).toInt() * elems
		val newIndex = types.size
		if (usage != null)
		{
			check(usage !in usages) { "already has usage $usage" }
			usages[usage] = newIndex
		}
		types += type
		byteOffsets += stride
		elementCounts += elems
		signedness += signed
		normalizedness += normalized
		locations += locationCounter

		locationCounter += 1
		stride += sizeof
	}

	private fun floatn (i:Int, u:String?=null) = custom(GL11C.GL_FLOAT, i, signed=true, normalized=false, u)

	fun float1 () = floatn(1)
	fun float2 () = floatn(2)
	fun float3 () = floatn(3)
	fun float4 () = floatn(4)

	fun location3D () = floatn(3, "location")
	fun normal () = floatn(3, "normal")
	fun uv (i:Int=0) = floatn(2, "uv$i")
	fun byteColor () = custom(GL11C.GL_UNSIGNED_BYTE, 4, signed=false, normalized=true, "color")

	fun build (): VertexFormat
	{
		val attrs = List(types.size) { i ->
			VertexFormat.Attribute(
				type=types[i],
				elementCount = elementCounts[i],
				byteOffset = byteOffsets[i],
				location = locations[i],
				index=i,
				normalized = normalizedness[i],
				signed = signedness[i],
			)
		}
		return VertexFormat(
			attributes = attrs,
			byUsage = usages.map { (n, i) -> n to attrs[i] }.toMap(),
			byteSize = stride,
		)
	}

}