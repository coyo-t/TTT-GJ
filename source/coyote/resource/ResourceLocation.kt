package coyote.resource

class ResourceLocation private constructor (val path: String)
{
	companion object
	{
		fun of (p:String): ResourceLocation
		{
			return ResourceLocation(p)
		}
	}
}