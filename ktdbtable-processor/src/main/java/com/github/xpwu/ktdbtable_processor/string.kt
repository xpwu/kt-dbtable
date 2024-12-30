package com.github.xpwu.ktdbtable_processor

import javax.lang.model.type.TypeMirror

fun String.align(indent: String): String {
  val ret = this.prependIndent(indent)
  // cut first indent
  return ret.substring(indent.length)
}

// 'ByteArray' .asType().toString() maybe is
// @org.jetbrains.annotations.NotNull byte[] or @org.jetbrains.annotations.Nullable byte[]
// so, toString() maybe return error result
fun TypeMirror.toTypeString(): String {
  return this.toString().trimEnd().substringAfterLast(" ")
}
