package coyote.lua

import coyote.readText
import coyote.resource.Resource
import party.iroiro.luajava.Lua
import party.iroiro.luajava.lua54.Lua54
import party.iroiro.luajava.value.LuaTableValue
import party.iroiro.luajava.value.LuaValue

class LuaCoyote: Lua54()
{
	fun runWithTableResult (r: Resource?): LuaTableValue?
	{
		// TODO input might be compiled chunk and not text
		r ?: return null
		val tell = top
		run(r.readText())
		val outs = get()
		top = tell
		return outs as? LuaTableValue
	}

	fun safeGetTable (from: LuaValue?): LuaTableValue
	{
		if (from != null && from is LuaTableValue)
			return from
		return LuaTableValue(this, Lua.LuaType.TABLE)
	}
}