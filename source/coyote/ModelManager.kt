package coyote

import coyote.geom.SavingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.resource.Resource
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d

class ModelManager (val resourceManager: ResourceManager)
{
	val FORMAT = buildVertexFormat {
		location3D()
		textureCoord()
		normal()
		byteColor()
	}

	private val L by lazy {
		LuaCoyote().also {
			it.openLibraries()
		}
	}

	fun loadModel (name: ResourceLocation)
	{
		TODO()
	}

	fun loadWavefront (src: Resource): List<TesselatorStore>
	{
		val lines = src.readTextLines().filterNot(String::isBlank).map(String::trim)

		val locations = mutableListOf<WavefrontVType>()
		val uvs = mutableListOf<Vector2d>()
		val normals = mutableListOf<Vector3d>()

		val commandLines = mutableListOf<Pair<String, String>>()

		for (line in lines)
		{
			val (command, payload) = line.split(' ', limit=2)
			when (command)
			{
				"#" -> continue
				"v" -> {
					locations += WavefrontVType().also {
						val attr = payload.toDoubles()
						it.location.set(attr[0], attr[1], attr[2])
						if (attr.size > 3)
						{
							it.color.x = attr[3]
							it.color.y = attr[4]
							it.color.z = attr[5]
							if (attr.size > 6)
							{
								it.color.w = attr[6]
							}
						}
					}
				}
				"vt" -> {
					uvs += Vector2d(payload.toDoubles())
				}
				"vn" -> {
					normals += Vector3d(payload.toDoubles())
				}
				else -> {
					commandLines += command to payload
				}
			}
		}

		val materialTesselators = mutableMapOf<String, Tesselator>()
		var currentTesselator = Tesselator().also {
			it.begin(FORMAT)
			materialTesselators[""] = it
		}

		for ((command, payload) in commandLines)
		{
			if (command == "usemtl")
			{
				currentTesselator = materialTesselators.getOrPut(payload) {
					Tesselator().also { it.begin(FORMAT) }
				}
				continue
			}
			if (command == "f")
			{
				val vertices = payload.split(' ')
				check(vertices.size >= 3) { "line or point" }
				if (vertices.size == 4)
				{
					TODO("quads")
				}
				for (vp in vertices.map { it.toVlakIndices() })
				{
					if (vp.size >= 3 && vp[2] != 0)
					{
						val n = normals[vp[2] - 1]
						currentTesselator.normal(n.x, n.y, n.z)
					}
					else
					{
						currentTesselator.normal(0, 1, 0)
					}
					if (vp.size >= 2 && vp[1] != 0)
					{
						val v = uvs[vp[1] - 1]
						currentTesselator.texture(v.x, v.y)
					}
					else
					{
						currentTesselator.texture(0, 0)
					}

					check(vp[0] != 0) { "no location??" }
					val v = locations[vp[0] - 1]
					currentTesselator.color(v.color.x, v.color.y, v.color.z, v.color.w)
					currentTesselator.vertex(v.location.x, v.location.y, v.location.z)
				}
				currentTesselator.triangle()
			}
		}

		val stomach = SavingTessDigester()
		val things = materialTesselators.values.map { it.end(stomach) }.filterNot { it.vertexCount == 0 }
		return things
	}

	private fun String.toDoubles (): DoubleArray
	{
		return split(' ').map { it.toDouble() }.toDoubleArray()
	}

	private fun String.toVlakIndices (): IntArray
	{
		return split("/").map { if (it.isBlank()) 0 else it.toInt() }.toIntArray()
	}

	private class WavefrontVType
	{
		val location = Vector3d()
		val color = Vector4d().apply { set(1.0) }
	}

	companion object
	{
		@JvmStatic
		fun main (vararg args: String)
		{
			INITIALATE_LIBRARIAN_SOULS()
			println(";)")
			val test = ModelManager(RESOURCES)
			test.loadWavefront(RESOURCES.get(ResourceLocation.of("mesh/player space.obj"))!!)
		}
	}

}