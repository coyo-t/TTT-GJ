package coyote

import org.lwjgl.opengl.GL45C.*

class Surface (val handle: Int)
{

	private var colorRenderBuffer = 0
	private var depthRenderBuffer = 0

	fun setColorAttachment (texture: Texture, mipLevel:Int=0)
	{
		glNamedFramebufferTexture(handle, GL_COLOR_ATTACHMENT0, texture.handle, mipLevel)
	}

	fun setColorAttachment (wide:Int, tall:Int)
	{
		if (colorRenderBuffer > 0) glDeleteRenderbuffers(colorRenderBuffer)
		colorRenderBuffer = glCreateRenderbuffers()
		glNamedRenderbufferStorage(colorRenderBuffer, GL_COLOR_COMPONENTS, wide, tall)
		glNamedFramebufferRenderbuffer(handle, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderBuffer)

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