package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class SQLiteAdapter(override val UnderlyingDB: SQLiteDatabase) : DBer<SQLiteDatabase> {
  override fun ExecSQL(sql: String) {
    UnderlyingDB.execSQL(sql)
  }

  override fun Query(query: String, bindArgs: Array<String>?): Cursor {
    return UnderlyingDB.rawQuery(query, bindArgs)
  }

  override fun Replace(table: String, values: ContentValues): Long {
    return UnderlyingDB.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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

//val sqlite = SQLiteDatabase.openOrCreateDatabase("", null)
//
//val db = DB(SQLiteAdapter(sqlite))
//
//fun a() {
//  db.UnderlyingDB.path
//}