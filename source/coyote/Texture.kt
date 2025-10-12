package coyote

import org.lwjgl.opengl.GL46C.*

class Texture (
	val wide: Int,
	val tall:Int,
	handle:Int,
)
{
	var handle: Int = handle

	fun uploadImage (pic: NativeImage)
	{
		nglTextureSubImage2D(
			handle,
			0,
			0, 0,
			pic.wide, pic.tall,
			GL_RGBA, GL_UNSIGNED_BYTE,
			pic.data.address(),
		)
	}

	val size get() = wide to tall

	fun setFilter (min:Int?=null, mag:Int?=null)
	{
		if (min != null) glTextureParameteri(handle, GL_TEXTURE_MIN_FILTER, min)
		if (mag != null) glTextureParameteri(handle, GL_TEXTURE_MAG_FILTER, mag)
	}

	fun setRepeat (x:Int?=null, y:Int?=null, z:Int?=null)
	{
		if (x != null) glTextureParameteri(handle, GL_TEXTURE_WRAP_S, x)
		if (y != null) glTextureParameteri(handle, GL_TEXTURE_WRAP_T, y)
		if (z != null) glTextureParameteri(handle, GL_TEXTURE_WRAP_R, z)
	}

}