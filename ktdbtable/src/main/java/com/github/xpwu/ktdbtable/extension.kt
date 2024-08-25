package com.github.xpwu.ktdbtable

fun String.noSqlKeyword(): String {
  return "`${this}`"
}

