package coyote

import coyote.lua.LuaCoyote
import coyote.lua.asInteger
import coyote.lua.asString
import coyote.ren.CompiledShaders
import coyote.resource.ResourceLocation
import coyote.resource.ResourceManager
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaTableValue
import java.io.InputStreamReader

val RESOURCES = ResourceManager(ASSETS_PATH)
val SHADERZ = CompiledShaders(RESOURCES)
val TEXTUREZ = TextureManager(RESOURCES)
val MESHEZ = OBJModelManager(RESOURCES)
val MODELZ = ModelManager(RESOURCES)

object SPRITEZ {
	private val sprites = mutableMapOf<ResourceLocation, Sprite>()

	private fun createLua (): LuaCoyote
	{
		val outs = LuaCoyote()
		outs.openLibraries()
		outs.push(::addSprite)
		outs.setGlobal("add_sprite")
		return outs
	}

	private val workingSprites = mutableListOf<Sprite>()

	private fun addSprite (L: Lua): Int
	{
		L.pushValue(1)
		val args = L.get()
		val sprName = ResourceLocation.of(args["name"].asString()!!)
		val sprPage = ResourceLocation.of(args["source"].asString()!!)
		val pSprSize = args["size"] as LuaTableValue
		val sprWide = pSprSize[1].asInteger().toInt()
		val sprTall = pSprSize[2].asInteger().toInt()

		val pSubImages = args["sub_images"] as LuaTableValue
		val subImages = pSubImages.map { (i,v) ->
			val fuck = v as LuaTableValue
			val p = fuck["co"] as LuaTableValue
			Sprite.SubImage(
				index = i.toInteger().toInt(),
				x = p[1].asInteger().toInt(),
				y = p[2].asInteger().toInt(),
			)
		}
		workingSprites += Sprite(sprName, sprPage, sprWide, sprTall, subImages)
		return 0
	}

	fun loadAllSprites (inWhat: ResourceLocation)
	{
		workingSprites.clear()
		createLua().use { L ->
			val res = requireNotNull(RESOURCES[inWhat]).readText()
			L.run(res)
			for (spr in workingSprites)
			{
				sprites[spr.name] = spr
			}
		}
	}

	fun loadSprite (name: ResourceLocation): Sprite
	{
		val FUCKUFCKFUCKUFKC = sprites.get(name)
		return checkNotNull(FUCKUFCKFUCKUFKC) {
			"ow. no sprite '$name'"
		}
	}


}

object FONTZ {
	val FZ = mutableMapOf<ResourceLocation, Font>()
	operator fun get (fontName: ResourceLocation): Font
	{
		if (fontName in FZ)
			return FZ.getValue(fontName)
		LuaCoyote().use { L ->
			L.openLibraries()
			val maybe = InputStreamReader(RESOURCES[fontName]!!.openInputStream()).use { stream ->
				L.run(stream.readText())
				L.get() as LuaTableValue
			}
			val picName = ResourceLocation.of(maybe["source"].toString())
			val pic = TEXTUREZ.imageManager[picName]
			L.push(maybe)
			L.getField(-1, "margin")
			val margin = if (L.isNumber(-1))
				L.toNumber(-1)
			else
				0.0
			L.pop(2)
			val rcpWide = 1.0 / pic.wide
			val rcpTall = 1.0 / pic.tall
			val xMargin = margin * rcpWide
			val yMargin = margin * rcpTall
			val keyColor = pic[0, 0]
			val charSet = maybe["chars"].toString()
			val charCount = charSet.length
			var mode = false
			var i = 0
			var j = 0
			val stop = pic.wide
			var currentChar = 0
			val glf = buildList {
				while (i < (stop+1) && currentChar < charCount)
				{
					val atEnd = i == stop
					val pixel = pic[i, 0]
					val whatStage = mode || atEnd
					if (whatStage)
					{
						if (atEnd || pixel == keyColor)
						{
							val uv0 = j * rcpWide
							val uv1 = i * rcpWide
							this += Font.Glyph(
								char = charSet[currentChar],
								patch = Rectangle(uv0+xMargin, yMargin, uv1-xMargin, 1.0-yMargin),
								advance = i-j,
								height = pic.tall,
							)
							currentChar += 1
							mode = false
						}
					}
					else
					{
						if (pixel != keyColor)
						{
							j = i
							mode = true
						}
					}

					i += 1
				}
			}
			return Font(TEXTUREZ.add(fontName, pic), glf, pic.tall).also {
				pic.close()
				FZ[fontName] = it
			}
		}
	}
}

