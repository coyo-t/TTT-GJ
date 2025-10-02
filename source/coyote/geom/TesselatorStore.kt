package coyote.geom

import coyote.applyVertexFormat
import org.lwjgl.opengl.GL46C.*
import java.lang.foreign.MemorySegment
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

class TesselatorStore(
	val format: VertexFormat,
	val vertexCount: Int,
	val indexCount: Int,
	val vertexRange: LongRange,
	val indexRange: LongRange,
	val data: MemorySegment,
): AutoCloseable
{

	val vertexByteSize get() = vertexRange.last-vertexRange.first+1
	val indexByteSize get() = indexRange.last-indexRange.first+1

	init
	{
		val p = Path("./guts/vbdump").normalize().toAbsolutePath().createParentDirectories()
		FileChannel.open(p, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { channel ->
			channel.write(data.asByteBuffer())
		}
	}

	private var vao = -1
	private var vb = 0
	private var ib = 0

	fun submit (pr:Int)
	{
		if (vertexCount == 0)
			return
		if (vao < 0)
		{
			vao = glCreateVertexArrays()
			vb = glCreateBuffers()
			ib = glCreateBuffers()
			nglNamedBufferStorage(vb, vertexByteSize, data.address(), 0)
			nglNamedBufferStorage(ib, indexByteSize, data.address()+indexRange.first, 0)
			glVertexArrayVertexBuffer(vao, 0, vb, 0L, format.byteSize)
			glVertexArrayElementBuffer(vao, ib)
			applyVertexFormat(format, vao)
		}
		glBindVertexArray(vao)
		glDrawElements(pr, indexCount, GL_UNSIGNED_INT, 0)
	}

	override fun close()
	{
		if (vao != 0)
		{
			glDeleteBuffers(ib)
			glDeleteBuffers(vb)
			glDeleteVertexArrays(vao)
		}
	}
}