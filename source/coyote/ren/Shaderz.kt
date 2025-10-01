package coyote.ren

import coyote.LuaCoyote
import coyote.RESOURCES
import coyote.resource.ResourceLocation
import party.iroiro.luajava.value.LuaTableValue
import java.io.InputStreamReader
import org.lwjgl.util.shaderc.Shaderc.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

class Shaderz
{
	private val L = LuaCoyote().apply {
		openLibraries()
	}
	private val compiler = shaderc_compiler_initialize()
	private val options = shaderc_compile_options_initialize()

	private val nametable = mutableMapOf<ResourceLocation, ShaderAsset>()

	fun loadShaderData (from: ResourceLocation): ShaderAsset
	{
		if (from in nametable)
			return nametable.getValue(from)
		val res = requireNotNull(RESOURCES[from])
		// TODO input might be precopmiled lua chunk not text
		val things = InputStreamReader(res.openInputStream()).use { f ->
			L.run(f.readText())
			L.get() as LuaTableValue
		}
		val vertexString = things["vertex"].toString()
		val fragmentString = things["fragment"].toString()

		shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5)
		shaderc_compile_options_set_source_language(options, shaderc_source_language_glsl)

		return ShaderAsset(
			vertex = run {
				val r = compileChunk(vertexString, shaderc_vertex_shader, from)
				getResultSeg(r).also { shaderc_result_release(r) }
			},
			fragment = run {
				val r = compileChunk(fragmentString, shaderc_fragment_shader, from)
				getResultSeg(r).also { shaderc_result_release(r) }
			},
		).also {
			nametable[from] = it
			L.top = 0
		}
	}

	private fun getResultSeg (r:Long): MemorySegment
	{
		val b = requireNotNull(shaderc_result_get_bytes(r))
		val c = shaderc_result_get_length(r)
		return Arena.ofAuto().allocate(c).copyFrom(MemorySegment.ofBuffer(b))
	}

	private fun compileChunk (ch: CharSequence, ty:Int, rl: ResourceLocation): Long
	{
		val thing = shaderc_compile_into_spv(
			compiler,
			ch,
			ty,
			"$ty: $rl",
			"main",
			options,
		)

		val errc = shaderc_result_get_num_errors(thing)

		check(errc <= 0) {
			(shaderc_result_get_error_message(thing) ?: "uhh!!?").also {
				shaderc_result_release(thing)
			}
		}

		return thing
	}

}