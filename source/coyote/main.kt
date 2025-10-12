package coyote

import coyote.window.WindowHint
import coyote.window.WindowManager
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.system.Configuration
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.invariantSeparatorsPathString

val RESOURCE_PATH = Path("./resources/").normalize().toAbsolutePath()
val ASSETS_PATH = RESOURCE_PATH/"assets"
val DATA_PATH = RESOURCE_PATH/"data"

fun INITIALATE_LIBRARIAN_SOULS ()
{
	// this is precarious >:/
	val LP = DATA_PATH/"dll/"
	loadSystemLibrary(LP/"lua5464.dll")
	Configuration.LIBRARY_PATH.set((LP/"org/lwjgl").normalize().toAbsolutePath().invariantSeparatorsPathString)
}

fun main (vararg args: String)
{
	var game: FPW? = null
	try
	{
		INITIALATE_LIBRARIAN_SOULS()
		WindowManager.init()
		println(":)")
		val window = with(WindowManager) {
			val windowSize = Vector2i(650, 450)
			hint(WindowHint.Defaults)
			hint(WindowHint.MajorContextVersion, 4)
			hint(WindowHint.MinorContextVersion, 6)
			hint(WindowHint.OpenGLProfile, GLFW_OPENGL_CORE_PROFILE)

			getVideoMode(primaryMonitor)?.let { l ->
				val w = l.width()
				val h = l.height()
				hint(WindowHint.LocationX, (w - windowSize.x) / 2)
				hint(WindowHint.LocationY, (h - windowSize.y) / 2)
			}

			hint(WindowHint.Resizable, true)
			hint(WindowHint.Visible, false)
			createWindow("MACHINE WITNESS", windowSize).also {
				it.setSizeLimits(
					minSize = 320 to 240,
					maxSize = null,
				)
			}
		}
		window.makeContextCurrent()
		drawInitialize()

		game = FPW(window)
		game.init()
		while (true)
		{
			game.preStep()
			WindowManager.pollEvents()
			game.step()
			game.draw()
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
	finally
	{
		game?.close()
		WindowManager.close()
	}

}
