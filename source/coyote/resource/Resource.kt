package coyote.resource

import coyote.LuaCoyote
import party.iroiro.luajava.value.LuaTableValue
import java.nio.channels.FileChannel
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.jvm.optionals.getOrNull

class Resource (val path: Path)
{
	private var meta: Optional<Map<String, String>>? = null
	fun getMetaData (): Map<String, String>?
	{
		val m = meta
		if (m != null)
			return m.getOrNull()
		val maybeMetaPath = Path("$path.lua")
		return try {
			LuaCoyote().use { L ->
				val run = Files.readString(maybeMetaPath)
				L.run(run)
				val res = L.get()
				if (res !is LuaTableValue)
					return emptyMap<String, String>().also { meta = Optional.of(it) }
				return res
					.entries
					.associate { (k, v) -> k.toString() to v.toString() }
					.also { meta = Optional.of(it) }
			}
		}
		catch (_: Exception)
		{
			null.also { meta = Optional.empty() }
		}
	}

	fun openInputStream (vararg options: OpenOption)
		= path.inputStream(*options)
	fun openChannel (vararg options: OpenOption)
		= FileChannel.open(path, *options)

	fun openZipFileSystem (): FileSystem
	{
		return FileSystems.newFileSystem(path, emptyMap<String,Any>(), null)
	}

}