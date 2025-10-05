package coyote

import java.awt.Color
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.util.function.Consumer

class NativeImage (val wide: Int, val tall:Int, val data:MemorySegment): AutoCloseable
{

	var freeImageCallback: (Consumer<Long>)? = null

	fun getIndex (atx:Int, aty:Int): Int
	{
		if (atx < 0 || atx >= wide || aty < 0 || aty >= tall)
			return -1
		return aty * wide + atx
	}

	operator fun get (x:Int, y:Int) = getPixel(x, y)

	fun getPixel (atx:Int, aty:Int): Color
	{
		val at = getIndex(atx, aty)
		if (at < 0)
			return Color(0, true)
		val addr = at * 4L
		return Color(
			data[JAVA_BYTE, addr].toInt() and 0xFF,
			data[JAVA_BYTE, addr+1].toInt() and 0xFF,
			data[JAVA_BYTE, addr+2].toInt() and 0xFF,
			data[JAVA_BYTE, addr+3].toInt() and 0xFF,
		)
	}

	override fun close()
	{
		freeImageCallback?.accept(data.address())
	}
}