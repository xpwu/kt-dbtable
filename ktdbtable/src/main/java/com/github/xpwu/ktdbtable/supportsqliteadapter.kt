package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class SupportSQLiteAdapter(override val UnderlyingDB: SupportSQLiteDatabase) :
  DBer<SupportSQLiteDatabase> {
  override fun ExecSQL(sql: String) {
    UnderlyingDB.execSQL(sql)
  }

  override fun Query(query: String, bindArgs: Array<String>?): Cursor {
    if (bindArgs == null) {
      return UnderlyingDB.query(query)
    }
    return UnderlyingDB.query(query, bindArgs)
  }

  override fun Replace(table: String, values: ContentValues): Long {
    return UnderlyingDB.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
  }

  override fun BeginTransaction() {
    UnderlyingDB.beginTransaction()
  }

  override fun SetTransactionSuccessful() {
    UnderlyingDB.setTransactionSuccessful()
  }

  override fun EndTransaction() {
    UnderlyingDB.endTransaction()
  }
}