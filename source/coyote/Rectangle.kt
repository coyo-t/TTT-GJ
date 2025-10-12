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


	operator fun component1 () = x0
	operator fun component2 () = y0
	operator fun component3 () = x1
	operator fun component4 () = y1

	companion object
	{
		fun ofSized (x:Number, y:Number, wide:Number, tall:Number): Rectangle
		{
			val x = x.toDouble()
			val y = y.toDouble()
			return Rectangle(x, y, x+wide.toDouble(), y+tall.toDouble())
		}
	}
}