package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase


interface DB : SupportSQLiteDatabase {
  interface Delegator {
    fun onProgress(current: Int, total: Int)
  }

  val Delegate: Delegator
    get() {
      return object : Delegator {
        override fun onProgress(current: Int, total: Int) {}
      }
    }
}

private const val sqlMaster = "sqlite_master"

fun DB.Exist(table: String): Boolean {
  val cursor =
    this.query("SELECT name FROM $sqlMaster WHERE type='table' AND name=?", arrayOf(table))
  return cursor.count == 1
}

fun DB.AllTables(): ArrayList<String> {
  val cursor = this.query("SELECT name FROM $sqlMaster WHERE type='table'")
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  return ret
}

fun DB.AllIndexes(table: String): ArrayList<String> {
  val cursor =
    this.query("SELECT name FROM $sqlMaster WHERE type='index' AND tbl_name=?", arrayOf(table))
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  return ret
}

fun DB.AllIndexes(): ArrayList<String> {
  val cursor = this.query("SELECT name FROM $sqlMaster WHERE type='index'")
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  return ret
}

private const val xmasterTable = "xdbtable_master"
private const val tableName = "table_name"
private const val tableVersion = "version"

fun DB.CreateXMaster() {
  if (this.Exist(xmasterTable)) {
    return
  }

  this.execSQL(
    "CREATE TABLE IF NOT EXISTS $xmasterTable ($tableName TEXT PRIMARY KEY NOT NULL, $tableVersion INTEGER)"
  )
}

fun DB.OldVersion(table: String): Int {
  this.CreateXMaster()
  val cursor: Cursor = this.query(
    "SELECT $tableVersion FROM $xmasterTable WHERE $tableName = ", arrayOf(table)
  )

  // default = 0
  var ret: Int = 0
  while (cursor.moveToNext() && !cursor.isNull(0)) {
    ret = cursor.getInt(0)
    break
  }

  cursor.close()
  return ret
}

fun MakePlaceHolder(len: Int): String {
  if (len == 0) {
    return ""
  }

  val builder = StringBuilder(len)
  builder.append("?")
  for (i in 1 until len) {
    builder.append(",?")
  }
  return builder.toString()
}

fun DB.OldVersions(tables: ArrayList<String>): IntArray {
  this.CreateXMaster()
  val placeholder = MakePlaceHolder(tables.size)
  val cursor: Cursor = this.query(
    "SELECT $tableVersion FROM $xmasterTable WHERE $tableName = ($placeholder)", arrayOf(tables)
  )

  // default = 0
  val ret = IntArray(tables.size)
  var i = 0
  while (cursor.moveToNext() && !cursor.isNull(0)) {
    ret[i] = cursor.getInt(0)
    ++i
  }

  cursor.close()
  return ret
}

fun DB.SetVersion(table: String, version: Int) {
  this.CreateXMaster()
  val cv = ContentValues(2)
  cv.put(tableName, table)
  cv.put(tableVersion, version)
  this.insert(xmasterTable, SQLiteDatabase.CONFLICT_REPLACE, cv)
}

fun DB.TableColumnNames(table: String): ArrayList<String> {
  val columnNames = ArrayList<String>()

  val cursor: Cursor = this.query("PRAGMA table_info(?)", arrayOf(table))

  while (cursor.moveToNext()) {
    columnNames.add(cursor.getString(1))
  }
  cursor.close()

  return columnNames
}


fun onOpen(db: DB) {
  // 1、读出所有的table
  val allTables = db.AllTables()
  // 2、对比所有的version  找出 migrator
  val oldVersions = db.OldVersions(allTables)

  data class MInfo(
    val migrations: List<Migration>,
    val tableName: String,
    val from: Int,
    val to: Int
  )

  val allMigrations = emptyList<MInfo>().toMutableList()
  for (i in 0 until allTables.size) {
    val nowV = Table.GetInfo(allTables[i])?.Version ?: continue
    if (nowV == oldVersions[i]) continue
    val res = Table.GetMigrations(allTables[i], oldVersions[i], nowV)
    if (res == null) {
      // todo Exception?
      Log.E("can NOT migrate table(${allTables[i]}) from ${oldVersions[i]} to $nowV")
      continue
    }
    allMigrations += MInfo(res, allTables[i], oldVersions[i], nowV)
  }
  // 3、对比出所有需要补充的字段
  val allAlterSQLs = emptyList<String>().toMutableList()
  for (table in allTables) {
    val oldColumns = db.TableColumnNames(table)
    val nowColumns = Table.GetInfo(table)?.Columns ?: emptyMap()
    val needAdders = nowColumns - oldColumns.toSet()
    for ((_, adder) in needAdders) {
      allAlterSQLs += adder
    }
  }
  // 4、对比出所有需要增加的索引
  val allAddIndexes = emptyList<String>().toMutableList()
  val oldIndexNames = db.AllIndexes().toSet()
  for (table in allTables) {
    val nowIndexes = Table.GetInfo(table)?.Indexes ?: emptyMap()
    val needAdders = nowIndexes - oldIndexNames
    for ((_, adder) in needAdders) {
      allAddIndexes += adder
    }
  }
  // 5、统计并回调进度
  val count = allMigrations.size + allAlterSQLs.size + allAddIndexes.size
  if (count == 0) {
    return
  }
  var done = 0
  db.Delegate.onProgress(done, count)

  db.beginTransaction()
  try {
    // 6、先执行migrator
    for ((ms, name, from, to) in allMigrations) {
      Log.I("migrate $name from $from to $to ...")
      for (mig in ms) {
        mig(TableBase(name, db))
        done++
        db.Delegate.onProgress(done, count)
      }
      // 6-2、更新版本号，即使每一个 mig 都做了更新，这里也再统一做一次，防止漏做
      // 在每一个mig的执行中，本也应该做一次更好，即使没做，也不影响最后的结果，也不会在下一次重新做
      // 因为这里是事务，所以不用考虑中间失败了，而没有及时做版本号更新的情况。
      db.SetVersion(name, to)
    }
    // 7、再alter add column 执行字段的补充，前面的migrator可能已经涉及这里的操作，所以需要重新判断。
    // 7-1、重新对比出所有需要补充的字段
    val oldDone = allAlterSQLs.size + done
    allAlterSQLs.clear()
    for (table in allTables) {
      val oldColumns = db.TableColumnNames(table)
      val nowColumns = Table.GetInfo(table)?.Columns ?: emptyMap()
      val needAdders = nowColumns - oldColumns.toSet()
      for ((_, adder) in needAdders) {
        allAlterSQLs += adder
      }
    }
    // 7-2、执行：  新的执行数量应该是 不大于之前的 alter 数量
    for (alter in allAlterSQLs) {
      Log.I(alter)
      db.execSQL(alter)
      done++
      db.Delegate.onProgress(done, count)
    }
    // 调整为之前的统计数
    done = oldDone
    db.Delegate.onProgress(done, count)

    // 8、最后执行 add index, 因为有 if not exists 所以可以直接执行
    for (index in allAddIndexes) {
      Log.I(index)
      db.execSQL(index)
      done++
      db.Delegate.onProgress(done, count)
    }
    db.setTransactionSuccessful()

    // 防御性代码
    if (done < count) {
      db.Delegate.onProgress(count, count)
    }
  } catch (e: Exception) {
    e.printStackTrace();
    throw e;
  } finally {
    db.endTransaction()
  }

  // todo 9、索引是否删除 需要区别对待 很可能是手动添加的，还可能是手动与自动都期望添加，如果要自动删除只能删除明确是自动添加的部分。
}

