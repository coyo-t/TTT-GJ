package coyote.ren

import coyote.LuaCoyote
import coyote.RESOURCES
import coyote.resource.ResourceLocation
import party.iroiro.luajava.value.LuaTableValue
import java.io.DataInputStream
import java.io.InputStreamReader

class Shaderz
{
	val L = LuaCoyote().apply {
		openLibraries()
	}

	fun loadShaderData (from: ResourceLocation)
	{
		val res = requireNotNull(RESOURCES[from])
		// TODO input might be precopmiled lua chunk not text
		val things = InputStreamReader(res.openInputStream()).use { f ->
			L.run(f.readText())
			L.get() as LuaTableValue
		}
		val vertexString = things["vertex"].toString()
		val fragmentString = things["fragment"].toString()
		println(vertexString)
		println(fragmentString)
		L.top = 0
	}

}