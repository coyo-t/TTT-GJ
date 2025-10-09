package coyote

import coyote.geom.SavingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Vector2d
import org.joml.Vector3d
import java.awt.Color
import java.nio.file.Files

class OBJModelManager(val resourceManager: ResourceManager)
{
	val FORMAT = buildVertexFormat {
		location3D()
		textureCoord()
		normal()
		byteColor()
	}
	private val nt = mutableMapOf<ResourceLocation, TesselatorStore>()
	private val error = mapOf("" to TesselatorStore.NON)

	operator fun get (rl: ResourceLocation): TesselatorStore
	{
		if (rl in nt)
			return nt.getValue(rl)

		try
		{
			val resource = requireNotNull(resourceManager[rl]) { "obj model file" }
			val lines = Files.readAllLines(resource.path).filterNot { it.startsWith("#") }
			val locations = mutableListOf<Vector3d>()
			val textures = mutableListOf<Vector2d>()
			val normals = mutableListOf<Vector3d>()
			val colors = mutableListOf<Color>()

			val indices = mutableListOf<IntArray>()
			val vertexCounts = mutableListOf<Int>()

			for (line in lines)
			{
				val parts = line.split(" ")
				when (parts[0])
				{
					"v" ->
					{
						locations += Vector3d(parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
						if ((parts.size - 1) > 3)
						{
							val colComps = parts.size - 1 - 3
							val r = parts[4].toFloat()
							val g = parts[5].toFloat()
							val b = parts[6].toFloat()
							val a = if (colComps >= 4) parts[7].toFloat() else 1f
							colors += Color(r, g, b, a)
						}
						else
						{
							if (colors.isNotEmpty())
							{
								colors += Color.WHITE
							}
						}
					}
					"vt" -> textures += Vector2d(parts[1].toDouble(), parts[2].toDouble())
					"vn" -> normals += Vector3d(parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
					"f" -> {
						val reeciesPeesies = parts.subList(1, parts.size)
						for (vtx in reeciesPeesies)
						{
							indices.add(vtx.split("/").map { (it.toIntOrNull() ?: 0) - 1 }.toIntArray())
						}
						vertexCounts += reeciesPeesies.size
					}
				}
			}
			with (Tesselator()) {
				begin(FORMAT)
				val hasColor = colors.isNotEmpty()
				var indexCounter = 0
				for (fc in vertexCounts)
				{
					for (j in 0..<fc)
					{
						val (v,vt,vn) = indices[indexCounter]
						if (vn >= 0)
						{
							val nn = normals[vn]
							normal(nn[0], nn[1], nn[2])
						}
						if (vt >= 0)
						{
							val nn = textures[vt]
							texture(nn[0], nn[1])
						}
						if (v >= 0)
						{
							val nn = locations[v]
							if (hasColor)
								color(colors[v])
							else
								color(Color.white)
							vertex(nn[0], nn[1], nn[2])
						}
						indexCounter += 1
					}
					when (fc)
					{
						3 -> triangle()
						4 -> quad()
						else -> TODO()
					}
				}
				return end(SavingTessDigester()).also {
					nt[rl] = it
				}
			}
		}
		catch (e: Exception)
		{
			//TODO bigass glowing "ERROR" model
			System.err.println("error loading obj model")
			e.printStackTrace()
			return TesselatorStore.NON.also { nt[rl] = it }
		}
	}

}