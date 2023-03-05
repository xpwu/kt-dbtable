package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

private const val masterTable = "ktdbtable_master"
private const val tableName = "table_name"
private const val tableVersion = "version"

fun DB.CreateMaster() {
  if (this.Exist(masterTable)) {
    return
  }

  this.execSQL(
    "CREATE TABLE IF NOT EXISTS $masterTable ($tableName TEXT PRIMARY KEY NOT NULL, $tableVersion INTEGER)")
}

fun DB.GetOldVersion(table: String): Int {
  val cursor: Cursor = this.query(
    "SELECT $tableVersion FROM $masterTable WHERE $tableName = ", arrayOf(table)
  )
  if (!cursor.moveToFirst() || cursor.isNull(0)) {
    cursor.close()
    // default = 0
    return 0
  }

  val ret: Int = cursor.getInt(0)
  cursor.close()
  return ret
}

fun DB.SetVersion(table: String, version: Int) {
  val cv = ContentValues(2)
  cv.put(tableName, table)
  cv.put(tableVersion, version)
  this.insert(masterTable, SQLiteDatabase.CONFLICT_REPLACE, cv)
}
