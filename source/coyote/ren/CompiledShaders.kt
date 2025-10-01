package coyote.ren

import coyote.SHADERS
import coyote.resource.ResourceLocation
import org.lwjgl.opengl.GL46C.*
import java.nio.ByteBuffer

class CompiledShaders
{
	private val nametable = mutableMapOf<ResourceLocation, ShaderPipeline>()

	operator fun get (rl: ResourceLocation): ShaderPipeline
	{
		if (rl in nametable)
			return nametable.getValue(rl)

		val shader = SHADERS.loadShaderData(rl)
		val vs = createShaderProgram(GL_VERTEX_SHADER, shader.vertex.asByteBuffer())
		val fs = createShaderProgram(GL_FRAGMENT_SHADER, shader.fragment.asByteBuffer())

		val pipeline = glCreateProgramPipelines()
		glUseProgramStages(pipeline, GL_VERTEX_SHADER_BIT, vs)
		glUseProgramStages(pipeline, GL_FRAGMENT_SHADER_BIT, fs)

		return ShaderPipeline(pipeline, vs, fs).also {
			nametable[rl] = it
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