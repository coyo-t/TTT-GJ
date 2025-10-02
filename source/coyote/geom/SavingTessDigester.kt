package coyote.geom

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

class SavingTessDigester: TesselatorDigester<TesselatorStore>()
{
	override fun digest(
		format: VertexFormat,
		data: MemorySegment,
		vertexCount: Int,
		indices: List<Int>
	): TesselatorStore
	{
		val iSize = indices.size * 4L
		val vSize = data.byteSize()
		val outd = Arena.ofAuto().allocate(vSize + iSize)

		MemorySegment.copy(
			data,
			0L,
			outd,
			0L,
			vSize,
		)
		MemorySegment.copy(
			MemorySegment.ofArray(indices.toIntArray()),
			0L,
			outd,
			vSize,
			iSize,
		)

		return TesselatorStore(
			format=format,
			vertexCount=vertexCount,
			indexCount=indices.size,
			vertexRange=0L..<vSize,
			indexRange=vSize..<outd.byteSize(),
			data=outd,
		)
	}

	override fun close()
	{
	}


}