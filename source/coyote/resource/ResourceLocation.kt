package coyote.resource

data class ResourceLocation private constructor (val path: String)
{

	override fun toString(): String
	{
		return path
	}

	companion object
	{
		fun of (p:String): ResourceLocation
		{
			return ResourceLocation(p)
		}
	}
}