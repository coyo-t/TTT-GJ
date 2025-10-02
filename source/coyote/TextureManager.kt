package coyote

import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.lwjgl.opengl.GL45C.*

class TextureManager(val resources: ResourceManager)
{

	val imageManager = PicManager(resources)

	private val nametable = mutableMapOf<ResourceLocation, Texture>()

	operator fun get (rl: ResourceLocation, discardImageData:Boolean=false): Texture
	{
		if (rl in nametable)
			return nametable.getValue(rl)

		try
		{
			val res = resources[rl]!!
			val pic = imageManager.loadImage(res)
			val t = glCreateTextures(GL_TEXTURE_2D)
			val wide = pic.wide
			val tall = pic.tall
			glTextureStorage2D(t, 1, GL_RGBA8, wide, tall)

			val meta = res.getMetaData() ?: emptyMap()
			val filter = when (meta["filter"])
			{
				"true", "linear" -> GL_LINEAR
				"false", "nearest" -> GL_NEAREST
				else -> GL_NEAREST
			}
			glTextureParameteri(t, GL_TEXTURE_MIN_FILTER, filter)
			glTextureParameteri(t, GL_TEXTURE_MAG_FILTER, filter)

			val repeat = when (meta["wrapping"])
			{
				"true", "repeat" -> GL_REPEAT
				"false", "clamp" -> GL_CLAMP_TO_EDGE
				"border" -> GL_CLAMP_TO_BORDER
				"mirror" -> GL_MIRRORED_REPEAT
				else -> GL_REPEAT
			}
			glTextureParameteri(t, GL_TEXTURE_WRAP_S, repeat)
			glTextureParameteri(t, GL_TEXTURE_WRAP_T, repeat)
			nglTextureSubImage2D(t, 0, 0, 0, wide, tall, GL_RGBA, GL_UNSIGNED_BYTE, pic.data.address())
			return Texture(wide, tall, t).also {
				nametable[rl] = it
				if (discardImageData)
					pic.close()
			}
		}
		catch (e: Exception)
		{
			TODO("missing/error texture")
		}
	}

}