package coyote.lua

import party.iroiro.luajava.value.LuaValue


fun LuaValue.asString (): String? {
	val L = state()
	val tell = L.top
	L.push(this)
	val outs = if (L.isString(-1))
		L.toString(-1)
	else
		null
	L.top = tell
	return outs
}

fun LuaValue.asBoolean (): Boolean {
	val L = state()
	val tell = L.top
	L.push(this)
	val outs = if (L.isBoolean(-1))
		L.toBoolean(-1)
	else
		false
	L.top = tell
	return outs
}

fun LuaValue.asNumber (): Double {
	val L = state()
	val tell = L.top
	L.push(this)
	val outs = if (L.isNumber(-1))
		L.toNumber(-1)
	else
		0.0
	L.top = tell
	return outs
}

fun LuaValue.asInteger (strict:Boolean=false): Long {
	val L = state()
	val tell = L.top
	L.push(this)
	val outs = if (L.isInteger(-1))
		L.toInteger(-1)
	else if (L.isNumber(-1) && !strict)
		L.toNumber(-1).toLong()
	else
		0L
	L.top = tell
	return outs
}
