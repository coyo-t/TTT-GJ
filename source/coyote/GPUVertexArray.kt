package coyote

import coyote.geom.VertexFormat
import org.lwjgl.opengl.GL46C.*

class GPUVertexArray(val format: VertexFormat)
{

	val handle = glCreateVertexArrays()

	private var vb = 0
	private var ib = 0

	init
	{
		applyVertexFormat(format, handle)
	}

	fun assignVertexSource (vb:Int)
	{
		this.vb = vb
		glVertexArrayVertexBuffer(handle, 0, vb, 0L, format.byteSize)
	}

	fun assignIndexSource (ib: Int)
	{
		this.ib = ib
		glVertexArrayElementBuffer(handle, ib)
	}

	fun close ()
	{
		glDeleteVertexArrays(handle)
	}

	fun bind ()
	{
		glBindVertexArray(handle)
	}
}