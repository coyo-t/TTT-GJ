package coyote

import java.lang.foreign.MemorySegment
import java.util.function.Consumer

class NativeImage (val wide: Int, val tall:Int, val data:MemorySegment)
{

	var freeImageCallback: (Consumer<Long>)? = null

}