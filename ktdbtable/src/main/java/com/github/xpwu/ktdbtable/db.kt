package com.github.xpwu.ktdbtable

import androidx.sqlite.db.SupportSQLiteDatabase

interface DB : SupportSQLiteDatabase {
  fun Exist(name: String):Boolean
}

