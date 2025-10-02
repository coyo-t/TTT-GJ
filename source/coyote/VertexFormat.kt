package coyote

class VertexFormat(
	val attributes: List<Attribute>,
	val byteSize: Int,
)
{



	data class Attribute (val type: Int)


}