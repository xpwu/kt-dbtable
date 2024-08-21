package com.github.xpwu.ktdbtable

import android.content.ContentValues

fun String.escape(): String {
  return this.replace("'", "''")
}

fun ContentValues.escape(): ContentValues {
  val ret = ContentValues(this.size())

  for (entry in this.valueSet()) {
    val key = "'" + entry.key.escape() + "'"
    // todo: putObject 的适用版本
    when (val value = entry.value) {
      null -> ret.putNull(key)
      is String -> ret.put(key, value)
      is Byte -> ret.put(key, value)
      is Short -> ret.put(key, value)
      is Int -> ret.put(key, value)
      is Long -> ret.put(key, value)
      is Float -> ret.put(key, value)
      is Double -> ret.put(key, value)
      is Boolean -> ret.put(key, value)
      is ByteArray -> ret.put(key, value)
      else -> throw IllegalArgumentException("Unsupported type " + value.javaClass)
    }
  }

  return ret
}