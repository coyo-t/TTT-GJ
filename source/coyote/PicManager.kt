package coyote

import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.lwjgl.stb.STBImage.nstbi_image_free
import org.lwjgl.stb.STBImage.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT
import java.nio.channels.FileChannel
import java.util.function.Consumer

class PicManager(val resources: ResourceManager)
{


	val picNametable = mutableMapOf<ResourceLocation, NativeImage>()

	operator fun get (rl: ResourceLocation): NativeImage
	{
		if (rl in picNametable)
			return picNametable.getValue(rl)

		val data = requireNotNull(resources[rl])
		data.openZipFileSystem().use { zip ->
			val maybePic = zip.getPath("mergedimage.png")
			FileChannel.open(maybePic).use { channel ->
				Arena.ofConfined().use { arena ->
					val fsize = channel.size()
					val extract = arena.allocate(fsize)
					channel.read(extract.asByteBuffer())
					val wp = arena.allocate(JAVA_INT)
					val hp = arena.allocate(JAVA_INT)
					val result = nstbi_load_from_memory(extract.address(), fsize.toInt(), wp.address(), hp.address(), 0L, 4)
					check(result != 0L) {
						"image load failz: ${stbi_failure_reason()}"
					}
					val wide = wp[JAVA_INT, 0L]
					val tall = hp[JAVA_INT, 0L]
					return NativeImage(
						wide,
						tall,
						MemorySegment.ofAddress(result).reinterpret(wide*tall*4L)
					).apply {
						freeImageCallback = Consumer { nstbi_image_free(it) }
						picNametable[rl] = this
					}
				}
			}
		}
	}
}