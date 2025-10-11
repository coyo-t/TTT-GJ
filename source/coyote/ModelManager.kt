package coyote

import coyote.geom.SavingTessDigester
import coyote.geom.Tesselator
import coyote.geom.TesselatorStore
import coyote.lua.LuaCoyote
import coyote.lua.asInteger
import coyote.lua.asString
import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaTableValue

class ModelManager (val resourceManager: ResourceManager)
{
	private val models = mutableMapOf<ResourceLocation, Model>()
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



	fun loadModel (name: ResourceLocation): Model
	{
		if (name in models)
			return models.getValue(name)

		val result = requireNotNull(L.runWithTableResult(resourceManager[name])) {
			TODO("error model")
		}
		val meshNameField = result["mesh"]
		val sourceLines = if (meshNameField is LuaTableValue)
		{
			val maybeSource = meshNameField["data"]
			if (maybeSource.type() != Lua.LuaType.NIL)
			{
				maybeSource.asString()!!.lines()
			}
			else
			{
				val maybePathTo = meshNameField["path"]
				if (maybePathTo.type() != Lua.LuaType.NIL)
				{
					resourceManager[ResourceLocation.of(maybePathTo.asString()!!)]!!.readTextLines()
				}
				else
				{
					null
				}
			}
		}
		else
		{
			null
		}
		requireNotNull(sourceLines) { "DINGUS @ '$name'" }

		val mesh = loadWavefront(sourceLines)

		var mIndex = 0
		val materialDefines = L.safeGetTable(result["materials"])
		val materials = materialDefines.values.associate { mDef ->
			val objName = mDef["obj_name"].asString()!!
			val shName = mDef["shader"].asString()!!
			val textureNames = L.safeGetTable(mDef["textures"])
			val textures = textureNames.map { (psi, ptn) ->
				psi.asInteger().toInt() to ResourceLocation.of(ptn.asString()!!)
			}
			val meshN = requireNotNull(mesh[objName]) {
				"no such material named '$objName' declared by wavefront"
			}
			meshN to MaterialDefine(
				mIndex,
				ResourceLocation.of(shName),
				textures,
			).also { mIndex += 1 }
		}
		return Model(name, mesh.values.toList(), materials).also { models[name] = it }
	}
	// this is really shitty
	class Model(
		val name: ResourceLocation,
		val meshes: List<TesselatorStore>,
		val materials: Map<TesselatorStore, MaterialDefine>
	)
	{
		val materialTextures = arrayOfNulls<List<Pair<Int, Texture>>>(materials.size)
		val materialShaders = arrayOfNulls<CompiledShaders.ShaderPipeline>(materials.size)

		fun draw (textureManager: TextureManager, shaderManager: CompiledShaders)
		{
			for (mesh in meshes)
			{
				val mdef = requireNotNull(materials[mesh]) {
					"no material for mesh $mesh (model '$name')"
				}
				val index = mdef.index
				val texture = materialTextures[index] ?: mdef.textures.map { (i,t) ->
					i to textureManager[t]
				}.also { materialTextures[index] = it }

				val shader = materialShaders[index] ?: shaderManager[mdef.shader].also { materialShaders[index] = it }
				drawSetShader(shader)
				for ((i,t) in texture)
				{
					drawBindTexture(i, t)
				}
				drawSubmit(mesh, GL_TRIANGLES)
			}
		}
	}

	class MaterialDefine (
		val index: Int,
		val shader: ResourceLocation,
		val textures: List<Pair<Int, ResourceLocation>>,
	)

	fun loadWavefront (srcLines: List<String>): Map<String, TesselatorStore>
	{
		val lines = srcLines.filterNot(String::isBlank).map(String::trim)

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
					val fuck = payload.toDoubles()
					uvs += Vector2d(fuck[0], 1.0 - fuck[1])
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
		return materialTesselators.map { (k,v) -> k to v.end(stomach) }.filterNot { (_,v) -> v.vertexCount == 0 }.toMap()
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
			test.loadModel(ResourceLocation.of("model/player space.lua"))
		}
	}

}