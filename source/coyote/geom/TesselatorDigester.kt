package coyote.geom

import java.lang.foreign.MemorySegment

abstract class TesselatorDigester <T>: AutoCloseable
{
	abstract fun digest (
		format: VertexFormat,
		data: MemorySegment,
		vertexCount: Int,
		indices: List<Int>,
	): T
}

