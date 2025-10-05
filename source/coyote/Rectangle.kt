package coyote

class Rectangle(
	var x0: Double,
	var y0: Double,
	var x1: Double,
	var y1: Double,
)
{

	val xSize get() = x1-x0
	val ySize get() = y1-y0
}