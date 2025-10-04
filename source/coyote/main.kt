package coyote

import coyote.window.WindowManager
import org.lwjgl.system.Configuration
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"

fun main (vararg args: String)
{
	// this is precarious >:/
	val LP = DATA_PATH/"dll/"
	loadSystemLibrary(LP/"lua5464.dll")
	Configuration.LIBRARY_PATH.set((LP/"org/lwjgl").normalize().toAbsolutePath().invariantSeparatorsPathString)
	WindowManager.init()

	println(":)")
	val game = FPW()
	game.init()

	try
	{
		while (true)
		{
			game.preStep()
			WindowManager.pollEvents()
			game.step()
		}
	}
	catch (_: StopGame)
	{
	}
	catch (e: Throwable)
	{
		e.printStackTrace()
	}
	catch (e: Exception)
	{
		e.printStackTrace()
	}

	WindowManager.close()
}
