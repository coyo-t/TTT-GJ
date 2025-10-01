package coyote.resource

import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.inputStream

class Resource (val path: Path)
{

	fun openInputStream (vararg options: OpenOption)
		= path.inputStream(*options)
	fun openChannel (vararg options: OpenOption)
		= FileChannel.open(path, *options)

}