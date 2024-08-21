package com.github.xpwu.ktdbtable.where

import android.content.ContentValues

fun String.escape(): String {
  return this.replace("'", "''")
}

//fun ContentValues.escape(): ContentValues {
//  val ret = ContentValues(this.size())
//
//}