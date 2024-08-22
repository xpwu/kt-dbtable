package com.github.xpwu.ktdbtable.where

fun String.escape(): String {
  return this.replace("'", "''")
}
