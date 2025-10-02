package coyote.ren

import java.lang.foreign.MemorySegment

class ShaderAsset(
	val vertex: MemorySegment,
	val fragment: MemorySegment,
)