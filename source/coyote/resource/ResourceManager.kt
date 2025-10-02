package coyote.resource

import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.jvm.optionals.getOrNull

class ResourceManager (root: Path)
{

	val root = root.normalize().toAbsolutePath()

	private val nametable = mutableMapOf<ResourceLocation, Optional<Resource>>()

	operator fun get (r: ResourceLocation): Resource?
	{
		if (r in nametable)
		{
			return nametable.getValue(r).getOrNull()
		}
		val pathTo = (root/r.path).normalize().toAbsolutePath()
		if (!pathTo.isRegularFile())
		{
			return null.also { nametable[r] = Optional.empty() }
		}
		return Resource(pathTo).also { nametable[r] = Optional.of(it) }
	}

}