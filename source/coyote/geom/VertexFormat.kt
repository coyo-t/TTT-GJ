package coyote.geom

data class VertexFormat(
	val attributes: List<Attribute>,
	val byUsage: Map<String, Attribute>,
	val byteSize: Int,
)
{
	data class Attribute (
		val type: Int,
		val elementCount: Int,
		val byteOffset: Int,
		val location: Int,
		val index: Int,
		val normalized: Boolean,
		val signed: Boolean,
	)


	companion object
	{
		val NON = VertexFormat(
			attributes = emptyList(),
			byUsage = emptyMap(),
			byteSize = 0,
		)
	}
}