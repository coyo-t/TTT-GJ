package coyote

import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.lwjgl.opengl.GL45C.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.JAVA_BYTE

class TextureManager(val resources: ResourceManager)
{

	val imageManager = PicManager(resources)

	private val nametable = mutableMapOf<ResourceLocation, Texture>()

	private val missingIMage by lazy {
		val sz = 8
		val pic = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
		val f = pic.createGraphics()
		f.color = Color.BLACK
		f.fillRect(0, 0, sz, sz)
		f.color = Color.MAGENTA
		val hs = sz / 2
		f.fillRect(0, 0, hs, hs)
		f.fillRect(hs, hs, hs, hs)
		f.dispose()
		val raster = pic.getRGB(0, 0, sz, sz, null, 0, sz)
		val rm = Arena.ofAuto().allocate(sz * sz *4L)
		for ((i,pix) in raster.withIndex())
		{
			val ii = i * 4L
			rm[JAVA_BYTE, ii] = ((pix ushr 16) and 0xFF).toByte()
			rm[JAVA_BYTE, ii + 1] = ((pix ushr 8) and 0xFF).toByte()
			rm[JAVA_BYTE, ii + 2] = (pix and 0xFF).toByte()
			rm[JAVA_BYTE, ii + 3] = ((pix ushr 24) and 0xFF).toByte()
		}
		NativeImage(sz, sz, rm)
	}
	val missingTexture by lazy {
		val w = missingIMage.wide
		val h = missingIMage.tall
		val t = glCreateTextures(GL_TEXTURE_2D)
		glTextureStorage2D(t, 1, GL_RGBA8, w, h)
		glTextureParameteri(t, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
		glTextureParameteri(t, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
		glTextureParameteri(t, GL_TEXTURE_WRAP_S, GL_REPEAT)
		glTextureParameteri(t, GL_TEXTURE_WRAP_T, GL_REPEAT)
		nglTextureSubImage2D(t, 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, missingIMage.data.address())
		Texture(w, h, t)
	}

	private var filter = GL_NEAREST
	private var repeat = GL_REPEAT

	private fun createGL (pic: NativeImage): Texture
	{
		try {
			val t = glCreateTextures(GL_TEXTURE_2D)
			val wide = pic.wide
			val tall = pic.tall
			glTextureStorage2D(t, 1, GL_RGBA8, wide, tall)
			glTextureParameteri(t, GL_TEXTURE_MIN_FILTER, filter)
			glTextureParameteri(t, GL_TEXTURE_MAG_FILTER, filter)
			glTextureParameteri(t, GL_TEXTURE_WRAP_S, repeat)
			glTextureParameteri(t, GL_TEXTURE_WRAP_T, repeat)
			nglTextureSubImage2D(t, 0, 0, 0, wide, tall, GL_RGBA, GL_UNSIGNED_BYTE, pic.data.address())
			return Texture(wide, tall, t)
		}
		catch (e: Exception)
		{
			System.err.println("problem creating image textuer")
			e.printStackTrace()
			return missingTexture
		}
	}

	fun add (underName: ResourceLocation, pic: NativeImage): Texture
	{
		filter = GL_NEAREST
		repeat = GL_REPEAT
		return createGL(pic).also {
			nametable[underName] = it
		}
	}

	operator fun get (rl: ResourceLocation, discardImageData:Boolean=false): Texture
	{
		if (rl in nametable)
			return nametable.getValue(rl)

		try
		{
			val res = requireNotNull(resources[rl]) { "no resource here" }
			val pic = imageManager.loadImage(res)

			val meta = res.getMetaData() ?: emptyMap()
			filter = when (meta["filter"])
			{
				"true", "linear" -> GL_LINEAR
				"false", "nearest" -> GL_NEAREST
				else -> GL_NEAREST
			}
			repeat = when (meta["wrapping"])
			{
				"true", "repeat" -> GL_REPEAT
				"false", "clamp" -> GL_CLAMP_TO_EDGE
				"border" -> GL_CLAMP_TO_BORDER
				"mirror" -> GL_MIRRORED_REPEAT
				else -> GL_REPEAT
			}
			return createGL(pic).also {
				nametable[rl] = it
				if (discardImageData)
					pic.close()
			}
		}
		catch (e: Exception)
		{
			System.err.println("image load $rl error:")
			e.printStackTrace()
			return missingTexture.also { nametable[rl] = it }
		}
	}

}