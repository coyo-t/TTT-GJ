package coyote

import org.lwjgl.opengl.GL45C.*

class Surface (val handle: Int)
{

	private var colorRenderBuffer = 0
	private var depthRenderBuffer = 0

	fun setColorAttachment (at:Int, texture: Texture, mipLevel:Int=0)
	{
		glNamedFramebufferTexture(handle, GL_COLOR_ATTACHMENT0 + at, texture.handle, mipLevel)
	}

	fun setDepthAttachment (wide:Int, tall:Int)
	{
		if (depthRenderBuffer > 0) glDeleteRenderbuffers(depthRenderBuffer)
		depthRenderBuffer = glCreateRenderbuffers()
		glNamedRenderbufferStorage(depthRenderBuffer, GL_DEPTH_COMPONENT, wide, tall)
		glNamedFramebufferRenderbuffer(handle, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBuffer)

	}

	fun free ()
	{
		if (colorRenderBuffer > 0) glDeleteRenderbuffers(colorRenderBuffer)
		if (depthRenderBuffer > 0) glDeleteRenderbuffers(depthRenderBuffer)
	}
}