package coyote.geom

import coyote.applyVertexFormat
import coyote.drawSubmit
import org.lwjgl.opengl.GL45C.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class RenderSubmittingTessDigester: TesselatorDigester<Unit>()
{
	private var mode = GL_TRIANGLES
	private var vao = 0
	private var vb = 0
	private var vbSize = 0L
	private var ib = 0
	private var ibSize = 0L

	private var iArena = Arena.ofConfined()
	private var iMemory = MemorySegment.NULL
	private var closed = false

	fun withMode (i:Int) = this.also { mode = i }

	private var pevFormat = VertexFormat.NON

	override fun digest(
		format: VertexFormat,
		data: MemorySegment,
		vertexCount: Int,
		indices: List<Int>,
	)
	{
		check(!closed)
		//TODO vb and ib data should use the
		// same buffer stored one after the other
		if (vertexCount < 0)
			return
		if (indices.isEmpty())
			return

		if (vao <= 0)
		{
			vao = glCreateVertexArrays()
		}

		val inVSize = data.byteSize()
		if (vbSize < inVSize)
		{
			if (vb != 0)
				glDeleteBuffers(vb)
			vb = glCreateBuffers()
			vbSize = inVSize
			glNamedBufferStorage(vb, inVSize, GL_DYNAMIC_STORAGE_BIT)
			pevFormat = VertexFormat.NON
		}
		if (pevFormat != format)
		{
			glVertexArrayVertexBuffer(vao, 0, vb, 0L, format.byteSize)
			applyVertexFormat(format, vao)
			pevFormat = format
		}

		val indexSize = 4L
		val inISize = indices.size * indexSize
		if (ibSize < inISize)
		{
			if (ib != 0)
				glDeleteBuffers(ib)
			ib = glCreateBuffers()
			ibSize = inISize
			glNamedBufferStorage(ib, inISize, GL_DYNAMIC_STORAGE_BIT)

			iArena.close()
			iArena = Arena.ofConfined()
			iMemory = iArena.allocate(ibSize)
			glVertexArrayElementBuffer(vao, ib)
		}

		for ((i,it) in indices.withIndex())
		{
			iMemory.setAtIndex(ValueLayout.JAVA_INT, i.toLong(), it)
		}

		nglNamedBufferSubData(ib, 0L, inISize, iMemory.address())
		nglNamedBufferSubData(vb, 0L, inVSize, data.address())

		drawSubmit(vao, mode, indices.size, GL_UNSIGNED_INT)
	}

	override fun close()
	{
		check(!closed)

		if (vb != 0)
			glDeleteBuffers(vb)
		if (ib != 0)
			glDeleteBuffers(ib)
		if (vao != 0)
			glDeleteVertexArrays(vao)
		closed = true
	}
}