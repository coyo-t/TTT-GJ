package coyote.ren

import coyote.LuaCoyote
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.util.shaderc.Shaderc.*
import party.iroiro.luajava.value.LuaTableValue
import java.io.InputStreamReader
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer

class CompiledShaders(val resources: ResourceManager)
{
	private val prNametable = mutableMapOf<ResourceLocation, ShaderPipeline>()

	operator fun get (rl: ResourceLocation): ShaderPipeline
	{
		if (rl in prNametable)
			return prNametable.getValue(rl)

		val shader = loadShaderData(rl)
		val vs = createShaderProgram(GL_VERTEX_SHADER, shader.vertex.asByteBuffer())
		val fs = createShaderProgram(GL_FRAGMENT_SHADER, shader.fragment.asByteBuffer())

		val pipeline = glCreateProgramPipelines()
		glUseProgramStages(pipeline, GL_VERTEX_SHADER_BIT, vs)
		glUseProgramStages(pipeline, GL_FRAGMENT_SHADER_BIT, fs)

		return ShaderPipeline(pipeline, vs, fs).also {
			prNametable[rl] = it
		}
	}

	private fun createShaderProgram (type:Int, code: ByteBuffer): Int
	{
		val shader = glCreateShader(type).also { check(it != 0) }
		glShaderBinary(intArrayOf(shader), GL_SHADER_BINARY_FORMAT_SPIR_V, code)
		glSpecializeShader(shader, "main", intArrayOf(), intArrayOf())
		val compiled = intArrayOf(GL_FALSE)
		glGetShaderiv(shader, GL_COMPILE_STATUS, compiled)
		check(compiled[0] == GL_TRUE)

		val program = glCreateProgram().also { check(it != 0) }
		glProgramParameteri(program, GL_PROGRAM_SEPARABLE, GL_TRUE)

		glAttachShader(program, shader)
		glLinkProgram(program)
		glDetachShader(program, shader)

		glDeleteShader(shader)
		return program
	}

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
		val res = requireNotNull(resources[from])
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

	class ShaderPipeline (
		val handle: Int,
		val vr: Int,
		val fr: Int,
	)
	{
		fun bind ()
		{
			glBindProgramPipeline(handle)
		}
	}
}