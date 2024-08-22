package com.github.xpwu.ktdbtable

fun String.notSqlKeyword(): String {
  return "`${this}`"
}

