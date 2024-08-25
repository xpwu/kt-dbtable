package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.github.xpwu.ktdbtable.where.Where

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


fun DB<SQLiteDatabase>.query(table: Table, where: Where): Cursor {
  return this.UnderlyingDB.query(table.SqlNameIn(this), null, where.ArgSQL
    , where.BindArgs, null, null, null)
}

fun DB<SQLiteDatabase>.query(table: Table, columns: Array<ColumnInfo>, where: Where): Cursor {
  val cls = Array(columns.size) { index: Int -> columns[index].toString() }

  return this.UnderlyingDB.query(table.SqlNameIn(this), cls, where.ArgSQL
    , where.BindArgs, null, null, null)
}

//fun SQLiteDatabase.query(tableName: String, columns: Array<ColumnInfo>, where: Where): Cursor {
//  val cls = Array(columns.size) { index: Int -> columns[index].toString() }
//
//  return this.query(tableName, cls, where.ArgSQL, where.BindArgs, null, null, null)
//}
//
//fun SQLiteDatabase.query(tableName: String, where: Where): Cursor {
//  return this.query(tableName, null, where.ArgSQL, where.BindArgs, null, null, null)
//}

//val sqlite = SQLiteDatabase.openOrCreateDatabase("", null)
//
//val db = DB(SQLiteAdapter(sqlite))
//
//fun a() {
//  db.UnderlyingDB.path
//}