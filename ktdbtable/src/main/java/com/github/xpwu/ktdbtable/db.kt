package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass

interface DBInner {
  val Path: String

  fun Query(query: String, bindArgs: Array<String>?): Cursor
  fun Insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long
  fun ExecSQL(sql: String)

  fun BeginTransaction()
  fun SetTransactionSuccessful()
  fun EndTransaction()
}

interface UnderlyingDBer<T> {
  val UnderlyingDB: T
}

interface DBer<T>: DBInner, UnderlyingDBer<T>

class DB<T>(internal val dber: DBer<T>, autoUpgrade: Boolean = true) : UnderlyingDBer<T> by dber {
  internal val tableCache = syncTableCache()
  internal var uContext: upgradeContext = upgradeContext()

  init {
    if (autoUpgrade) {
      this.OnOpen()
      this.OnUpgrade()
    }
  }

  val UpgradeCount: Int get() = this.OnOpen()

  /**
   * @Upgrade 如果 autoUpgrade == false，必须在后续流程中调用 Upgrade 方法
   * @param onProgress 进度回调
   * @param total 总数
   */
  fun Upgrade(onProgress: (Int)->Unit, total: (Int)->Unit = {}) {
    // 需要重新OnOpen，确保是最新的状态
    total(this.OnOpen())
    this.OnUpgrade(onProgress)
  }
}

class SQLiteAdapter(override val UnderlyingDB: SQLiteDatabase) : DBer<SQLiteDatabase> {
  override val Path: String = UnderlyingDB.path
  override fun ExecSQL(sql: String) {
    UnderlyingDB.execSQL(sql)
  }

  override fun Query(query: String, bindArgs: Array<String>?): Cursor {
    return UnderlyingDB.rawQuery(query, bindArgs)
  }

  override fun Insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
    return UnderlyingDB.insertWithOnConflict(table, null, values, conflictAlgorithm)
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

class SupportSQLiteAdapter(override val UnderlyingDB: SupportSQLiteDatabase) : DBer<SupportSQLiteDatabase> {
  override val Path: String = UnderlyingDB.path
  override fun ExecSQL(sql: String) {
    UnderlyingDB.execSQL(sql)
  }

  override fun Query(query: String, bindArgs: Array<String>?): Cursor {
    return UnderlyingDB.query(query, bindArgs)
  }

  override fun Insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
    return UnderlyingDB.insert(table, conflictAlgorithm, values)
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

class syncTableCache {
  private val tableCache: MutableSet<String> = emptySet<String>().toMutableSet()
  private val lock = ReentrantReadWriteLock()

  fun Has(tableName: String): Boolean {
    val rLock = lock.readLock()
    rLock.lock()
    val ret = tableCache.contains(tableName)
    rLock.unlock()
    return ret
  }

  fun Add(tableName: String) {
    val wLock = lock.writeLock()
    wLock.lock()
    tableCache.add(tableName)
    wLock.unlock()
  }

  fun Clear(tableName: String) {
    val wLock = lock.writeLock()
    wLock.lock()
    tableCache.remove(tableName)
    wLock.unlock()
  }

  fun Clear() {
    val wLock = lock.writeLock()
    wLock.lock()
    tableCache.clear()
    wLock.unlock()
  }
}

fun DB<*>.OnlyForInitTable(sqls: List<String>) {
  for (sql in sqls) {
    this.dber.ExecSQL(sql)
  }
}

private const val sqlMaster = "sqlite_master"

fun DB<*>.Exist(table: String): Boolean {
  if (this.tableCache.Has(table)) {
    return true
  }

  val cursor =
    this.dber.Query("SELECT name FROM $sqlMaster WHERE type='table' AND name=?", arrayOf(table))
  val ret: Boolean = cursor.count == 1

  if (ret) {
    this.tableCache.Add(table)
  }

  return ret
}

fun DB<*>.AllTables(): ArrayList<String> {
  this.tableCache.Clear()
  val cursor = this.dber.Query("SELECT name FROM $sqlMaster WHERE type='table'", null)
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
    this.tableCache.Add(cursor.getString(0))
  }
  return ret
}

fun DB<*>.AllIndexes(table: String): ArrayList<String> {
  val cursor =
    this.dber.Query("SELECT name FROM $sqlMaster WHERE type='index' AND tbl_name=?", arrayOf(table))
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  return ret
}

fun DB<*>.AllIndexes(): ArrayList<String> {
  val cursor = this.dber.Query("SELECT name FROM $sqlMaster WHERE type='index'", null)
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  return ret
}

private const val xmasterTable = "xdbtable_master"
private const val tableName = "table_name"
private const val tableVersion = "version"

fun DB<*>.CreateXMaster() {
  if (this.Exist(xmasterTable)) {
    return
  }

  this.dber.ExecSQL(
    "CREATE TABLE IF NOT EXISTS $xmasterTable ($tableName TEXT PRIMARY KEY NOT NULL, $tableVersion INTEGER)"
  )
}

fun DB<*>.OldVersion(table: String): Int {
  this.CreateXMaster()
  val cursor: Cursor = this.dber.Query(
    "SELECT $tableVersion FROM $xmasterTable WHERE $tableName = ?", arrayOf(table)
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

fun DB<*>.OldVersions(tables: ArrayList<String>): IntArray {
  this.CreateXMaster()
  val placeholder = MakePlaceHolder(tables.size)
  val cursor: Cursor = this.dber.Query(
    "SELECT $tableVersion FROM $xmasterTable WHERE $tableName = ($placeholder)",
    tables.toArray(emptyArray())
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

fun DB<*>.SetVersion(table: String, version: Int) {
  this.CreateXMaster()
  val cv = ContentValues(2)
  cv.put(tableName, table)
  cv.put(tableVersion, version)
  this.dber.Insert(xmasterTable, SQLiteDatabase.CONFLICT_REPLACE, cv)
}

fun DB<*>.TableColumnNames(table: String): ArrayList<String> {
  val columnNames = ArrayList<String>()

  val cursor: Cursor = this.dber.Query("PRAGMA table_info(?)", arrayOf(table))

  while (cursor.moveToNext()) {
    columnNames.add(cursor.getString(1))
  }
  cursor.close()

  return columnNames
}

internal data class MInfo(
  val migrations: List<Migration>,
  val tableName: String,
  val from: Int,
  val to: Int
)

internal class upgradeContext(
  val allTables: List<String> = emptyList(),
  val allMigrations: List<MInfo> = emptyList(),
  val allAlterSQLs: List<String> = emptyList(),
  val allAddIndexes: List<String> = emptyList(),
) {
  val count: Int get() = allMigrations.size + allAlterSQLs.size + allAddIndexes.size
}

// 返回需要运行的升级数量，0 表示不需要升级
// 如果返回的不为0，则在后续的流程中必须执行 OnUpgrade
private fun DB<*>.OnOpen(): Int {
  // 1、读出所有的table
  val allTables = this.AllTables()
  // 2、对比所有的version  找出 migrator
  val oldVersions = this.OldVersions(allTables)

  val allMigrations = emptyList<MInfo>().toMutableList()
  for (i in 0 until allTables.size) {
    val nowV = Table.GetInfo(allTables[i])?.Version ?: continue
    if (nowV == oldVersions[i]) continue
    val res = Table.GetMigrations(allTables[i], oldVersions[i], nowV)
      ?: throw SQLException("can NOT migrate table(${allTables[i]}) from ${oldVersions[i]} to $nowV")
    allMigrations += MInfo(res, allTables[i], oldVersions[i], nowV)
  }
  // 3、对比出所有需要补充的字段
  val allAlterSQLs = emptyList<String>().toMutableList()
  for (table in allTables) {
    val oldColumns = this.TableColumnNames(table)
    val nowColumns = Table.GetInfo(table)?.Columns ?: emptyMap()
    val needAdders = nowColumns - oldColumns.toSet()
    for ((_, adder) in needAdders) {
      allAlterSQLs += adder
    }
  }
  // 4、对比出所有需要增加的索引
  val allAddIndexes = emptyList<String>().toMutableList()
  val oldIndexNames = this.AllIndexes().toSet()
  for (table in allTables) {
    val nowIndexes = Table.GetInfo(table)?.Indexes ?: emptyMap()
    val needAdders = nowIndexes - oldIndexNames
    for ((_, adder) in needAdders) {
      allAddIndexes += adder
    }
  }
  // 5、统计
  this.uContext = upgradeContext(allTables, allMigrations, allAlterSQLs, allAddIndexes)
  return allMigrations.size + allAlterSQLs.size + allAddIndexes.size
}

private fun DB<*>.OnUpgrade(onProgress: (Int)->Unit = {}) {
  val count = this.uContext.allMigrations.size +
    this.uContext.allAlterSQLs.size + this.uContext.allAddIndexes.size
  var done = 0

  onProgress(done)
  if (count == 0) {
    return
  }

  this.dber.BeginTransaction()
  try {
    // 6、先执行migrator
    for ((ms, name, from, to) in this.uContext.allMigrations) {
      Log.I("migrate $name from $from to $to ...")
      for (mig in ms) {
        mig(this)
        done++
        onProgress(done)
      }
      // 6-2、更新版本号，即使每一个 mig 都做了更新，这里也再统一做一次，防止漏做
      // 在每一个mig的执行中，本也应该做一次更好，即使没做，也不影响最后的结果，也不会在下一次重新做
      // 因为这里是事务，所以不用考虑中间失败了，而没有及时做版本号更新的情况。
      this.SetVersion(name, to)
    }
    // 7、再alter add column 执行字段的补充，前面的migrator可能已经涉及这里的操作，所以需要重新判断。
    // 7-1、重新对比出所有需要补充的字段
    val oldDone = this.uContext.allAlterSQLs.size + done
    val allAlterSQLs = emptyList<String>().toMutableList()
    for (table in this.uContext.allTables) {
      val oldColumns = this.TableColumnNames(table)
      val nowColumns = Table.GetInfo(table)?.Columns ?: emptyMap()
      val needAdders = nowColumns - oldColumns.toSet()
      for ((_, adder) in needAdders) {
        allAlterSQLs += adder
      }
    }
    // 7-2、执行：  新的执行数量应该是 不大于之前的 alter 数量
    for (alter in allAlterSQLs) {
      Log.I(alter)
      this.dber.ExecSQL(alter)
      done++
      onProgress(done)
    }
    // 调整为之前的统计数
    done = oldDone
    onProgress(done)

    // 8、最后执行 add index, 因为有 if not exists 所以可以直接执行
    for (index in this.uContext.allAddIndexes) {
      Log.I(index)
      this.dber.ExecSQL(index)
      done++
      onProgress(done)
    }
    this.dber.SetTransactionSuccessful()

    // 防御性代码
    if (done < count) {
      onProgress(count)
    }
  } catch (e: Exception) {
    e.printStackTrace();
    throw e;
  } finally {
    this.dber.EndTransaction()
  }

  this.uContext = upgradeContext()

  // todo 9、索引是否删除 需要区别对待 很可能是手动添加的，还可能是手动与自动都期望添加，如果要自动删除只能删除明确是自动添加的部分。
}

