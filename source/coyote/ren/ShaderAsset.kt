package coyote.ren

import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer

class ShaderAsset(
	val vertex: MemorySegment,
	val fragment: MemorySegment,
)
{
}