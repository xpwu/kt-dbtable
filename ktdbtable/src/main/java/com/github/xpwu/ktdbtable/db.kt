package com.github.xpwu.ktdbtable

import androidx.sqlite.db.SupportSQLiteDatabase


interface DB : SupportSQLiteDatabase {
  interface Delegate {
    fun onProgress(current: Int, total: Int)
  }

  fun Exist(table: String): Boolean
  fun Version(table: String): Int
  fun SetVersion(table: String): Int
}


fun onOpen(db: DB) {

}

