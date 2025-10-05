package coyote


import org.joml.Vector2ic
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


class ScreenScalar (refWide:Int, refTall:Int)
{
	private val CBASEWIDE = refWide
	private val CBASETALL = refTall
	private val RCP_CBWIDE = 1.0 / CBASEWIDE
	private val RCP_CBTALL = 1.0 / CBASETALL

	var sizeProvider: (() -> Vector2ic)? = null

	var preferredScaleProvider: (() -> Int)? = null

	var wide: Int = 1
		private set

	var tall: Int = 1
		private set

	var scale: Int = 1
		private set

	var inverseScale: Double = 1.0
		private set

	fun update ()
	{
		val viewSize = sizeProvider?.invoke() ?: return
		val inscale = preferredScaleProvider?.invoke() ?: 1

		val inwide = viewSize.x()
		val intall = viewSize.y()
		scale = max(floor(min(inwide * RCP_CBWIDE, intall * RCP_CBTALL)).toInt(), 1)

		if (inscale > 0)
			scale = min(scale.toDouble(), inscale.toDouble()).toInt()

		inverseScale = 1.0 / scale
		wide  = ceil(inwide * inverseScale).toInt()
		tall = ceil(intall * inverseScale).toInt()
	}

}
