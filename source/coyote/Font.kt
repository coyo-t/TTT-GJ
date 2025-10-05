package coyote

class Font(
	val texture: Texture,
	val glyphs: List<Glyph>
)
{
	private val glyphsByChar = mutableMapOf<Char, Glyph>()
	private val primaryGlyphs = run {
		val ch = arrayOfNulls<Glyph>(256)
		for (gl in glyphs)
		{
			val cc = gl.char.code
			if (cc in 0..0xFF)
			{
				ch[cc] = gl
			}
			else
			{
				glyphsByChar[gl.char] = gl
			}
		}
		ch
	}

	operator fun get (ch: Char): Glyph?
	{
		val n = ch.code
		if (n in 0..0xFF)
			return primaryGlyphs[n]
		return glyphsByChar[ch]
	}

	class Glyph(
		val char: Char,
		val patch: Rectangle,
		val advance: Int,
		val height: Int,
	)
}