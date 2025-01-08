package com.github.xpwu.ktdbtable

import android.content.ContentValues
import android.database.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass

interface DBInner {
  companion object {
    // 每个值的意义与 android.database.sqlite.SQLiteDatabase 中的意义一样
    // 在本接口的 Insert 实现中，需要实现 CONFLICT_REPLACE 的冲突定义
//    val CONFLICT_NONE = 0
//    val CONFLICT_ROLLBACK = 1
//    val CONFLICT_ABORT = 2
//    val CONFLICT_FAIL = 3
//    val CONFLICT_IGNORE = 4
//    const val CONFLICT_REPLACE = 5
  }

  fun Query(query: String, bindArgs: Array<String>?): Cursor
  fun Replace(table: String, values: ContentValues): Long
  fun ExecSQL(sql: String)

  fun BeginTransaction()
  fun SetTransactionSuccessful()
  fun EndTransaction()
}

interface UnderlyingDBer<T> {
  val UnderlyingDB: T
}

interface DBer<T> : DBInner, UnderlyingDBer<T>

typealias TableBinding = Pair<KClass<*>, String>

fun MakeBinding(kclazz: KClass<*>, rename: String): TableBinding {
  return Pair(kclazz, rename)
}

/**
 * @param tablesBinding 明确binding 一个 table在此db中，也可以作为某一个table在此db中的重命名
 */
class DB<T>(internal val dber: DBer<T>, tablesBinding: List<TableBinding> = emptyList()) : UnderlyingDBer<T> by dber {
  internal val tableCache = syncTableCache()
  internal var uContext: upgradeContext = upgradeContext()

  internal val binding2name: Map<KClass<*>, String>
  internal val name2binding: Map<String, KClass<*>>

  // String: real table name
  internal val opened: syncMutableSet<String> = syncMutableSet()

  init {
    val binding2nameT: MutableMap<KClass<*>, String> = mutableMapOf()
    val name2bindingT: MutableMap<String, KClass<*>> = mutableMapOf()

    for (tableBinding in tablesBinding) {
      val name = tableBinding.second
      val value = tableBinding.first
      val old = name2bindingT[name]
      if (old == value) {
        continue
      }
      if (old != null) {
        throw Exception("table name(${name}) is duplicate, which is used by $value and $old!")
      }

      name2bindingT[name] = value
      binding2nameT[value] = name
    }
    binding2name = binding2nameT
    name2binding = name2bindingT
  }
}

typealias TableIfLessVersion = Pair<KClass<*>, Int>
fun UpTable(table: KClass<*>, ifLessVersion: Int): TableIfLessVersion = Pair(table, ifLessVersion)

// total 只会被调用一次；onProgress 会被多次调用,最后一次调用返回的值为total返回的值
// onProgress 调用结束后，Upgrade 函数才会运行结束
// tables 中指定的 table 需要在满足以下条件时，才会被Upgrade执行升级，如果某张表不满足条件，后续在首次使用时，会自动升级(如果需要升级的话)
//  1、table 已经在 db 中存在；
//  2、table 在 db 中的历史版本号 小于 ifLessVersion；
//  3、table 在代码中指定的版本号  大于或等于 ifLessVersion。
suspend fun DB<*>.Upgrade(tables: List<TableIfLessVersion>, onProgress: (Int) -> Unit, total: (Int) -> Unit) {
  // 找出所有的需要升级的table
  val list = emptyList<Migration>().toMutableList()
  for (table in tables) {
    val info = GetTableInfo(table.first)
    val name = this.Name(table.first)?:info.DefaultName
    val oldV = this.OldVersion(name)
    // table 还不存在的情况，也不用做 Migrate
    if (this.Exist(name) && table.second > oldV && table.second <= info.Version) {
      list.addAll(FindBestMigratorPath(oldV, info.Version, info.Migrators))
      this.OpenAndUpgrade(table.first, info, false)
    }
  }

  val ch = Channel<Int>(UNLIMITED)
  // find total
  CoroutineScope(Dispatchers.IO).launch {
    var count = 0
    for (mig in list) {
      count += mig.Count(this@Upgrade)
    }
    ch.send(count)
  }
  val totalV = ch.receive()
  total(totalV)

  // exe
  CoroutineScope(Dispatchers.IO).launch {
    var done = 0
    for (mig in list) {
      var thisCnt = 0
      mig.Exe(this@Upgrade) {
        launch {
          ch.send(done + it)
        }
        thisCnt = it
      }
      done += thisCnt
    }
  }

  var last = 0
  for (p in ch) {
    // launch {ch.send} 并不能确定顺序性，但onProgress()回调的值必须递增
    if (p > last) {
      onProgress(p)
      last = p
    }

    if (p >= totalV) {
      break
    }
  }

}

class syncMutableSet<E> {
  private val values: MutableSet<E> = emptySet<E>().toMutableSet()
  private val lock = ReentrantReadWriteLock()

  fun add(element: E): Boolean {
    val wLock = lock.writeLock()
    wLock.lock()
    val ret = values.add(element)
    wLock.unlock()
    return ret
  }

  fun addAll(elements: Collection<E>): Boolean {
    val wLock = lock.writeLock()
    wLock.lock()
    val ret = values.addAll(elements)
    wLock.unlock()
    return ret
  }

  fun isEmpty(): Boolean {
    val rLock = lock.readLock()
    rLock.lock()
    val ret = values.isEmpty()
    rLock.unlock()
    return ret
  }

  fun contains(element: @UnsafeVariance E): Boolean {
    val rLock = lock.readLock()
    rLock.lock()
    val ret = values.contains(element)
    rLock.unlock()
    return ret
  }

  fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
    val rLock = lock.readLock()
    rLock.lock()
    val ret = values.containsAll(elements)
    rLock.unlock()
    return ret
  }
}

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

fun DB<*>.OnlyForInitTable(exe: (db: DBInner)->Unit) {
  exe(this.dber)
}

fun DB<*>.Name(table: KClass<*>): String? {
  return binding2name[table]
}

private const val sqlMaster = "sqlite_master"

fun DB<*>.Exist(table: String): Boolean {
  if (this.tableCache.Has(table)) {
    return true
  }

  val cursor =
    this.dber.Query("SELECT name FROM $sqlMaster WHERE type='table' AND name=?", arrayOf(table))
  val ret: Boolean = cursor.count == 1
  cursor.close()

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
  cursor.close()

  return ret
}

fun DB<*>.AllIndexes(table: String): ArrayList<String> {
  val cursor =
    this.dber.Query("SELECT name FROM $sqlMaster WHERE type='index' AND tbl_name=?", arrayOf(table))
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  cursor.close()
  return ret
}

fun DB<*>.AllIndexes(): ArrayList<String> {
  val cursor = this.dber.Query("SELECT name FROM $sqlMaster WHERE type='index'", null)
  val ret = ArrayList<String>()
  while (cursor.moveToNext()) {
    ret.add(cursor.getString(0))
  }
  cursor.close()
  return ret
}

const val XMasterTable = "xdbtable_master"
const val XMasterTblNameColumn = "table_name"
const val XMasterTblVersionColumn = "version"

fun DB<*>.CreateXMaster() {
  if (this.Exist(XMasterTable)) {
    return
  }

  this.dber.ExecSQL(
    "CREATE TABLE IF NOT EXISTS $XMasterTable ($XMasterTblNameColumn TEXT PRIMARY KEY NOT NULL, $XMasterTblVersionColumn INTEGER)"
  )
}

fun DB<*>.OldVersion(table: String): Int {
  this.CreateXMaster()
  val cursor: Cursor = this.dber.Query(
    "SELECT $XMasterTblVersionColumn FROM $XMasterTable WHERE $XMasterTblNameColumn = ?",
    arrayOf(table)
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
    "SELECT $XMasterTblNameColumn, $XMasterTblVersionColumn FROM $XMasterTable WHERE $XMasterTblNameColumn in ($placeholder)",
    tables.toArray(emptyArray())
  )

  // default = 0
  val curRet = emptyMap<String, Int>().toMutableMap()
  while (cursor.moveToNext() && !cursor.isNull(0)) {
    curRet[cursor.getString(0)] = cursor.getInt(1)
  }
  cursor.close()

  val ret = IntArray(tables.size)
  for ((i, table) in tables.withIndex()) {
    ret[i] = curRet[table]?:0
  }
  return ret
}

fun DB<*>.SetVersion(table: String, version: Int) {
  this.CreateXMaster()
  val cv = ContentValues(2)
  cv.put(XMasterTblNameColumn, table)
  cv.put(XMasterTblVersionColumn, version)
  this.dber.Replace(XMasterTable, cv)
}

fun DB<*>.TableColumnNames(table: String): ArrayList<String> {
  val columnNames = ArrayList<String>()

  val cursor: Cursor = this.dber.Query("PRAGMA table_info(`$table`)", null)

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

// 找出Table.GetInfo的真实key，如果有重复，Table.GetInfo 中的 key 为 kclass.qualifiedName
private fun DB<*>.unBinding(name: String): String {
  return this.name2binding[name]?.qualifiedName?:name
}

fun DB<*>.Open(table: String) {
  this.opened.add(table)
}

fun DB<*>.OpenAndUpgrade(tableClazz: KClass<*>, info: TableInfo, includeMigrations: Boolean = true) {
  // 1、name
  val name = this.Name(tableClazz)?:info.DefaultName
  if (this.opened.contains(name)) {
    return
  }
  this.opened.add(name)

  this.dber.BeginTransaction()
  try {
    // 先完成自动升级的内容，手动升级的代码中可能使用了自动升级后的字段

    // 2、对比出所有需要补充的字段
    val allAlterSQLs = emptyList<String>().toMutableList()
    val oldColumns = this.TableColumnNames(name)
    val needAdders = info.Columns - oldColumns.toSet()
    for ((_, adder) in needAdders) {
      allAlterSQLs += adder
    }
    // alter add column 执行字段的补充
    for (alter in allAlterSQLs) {
      Log.I(alter)
      this.dber.ExecSQL(alter)
    }

    // 3、对比出所有需要增加的索引
    val allAddIndexes = emptyList<String>().toMutableList()
    val oldIndexNames = this.AllIndexes().toSet()
    val needAdders2 = info.Indexes - oldIndexNames
    for ((_, adder) in needAdders2) {
      allAddIndexes += adder
    }
    // 执行 add index, 因为有 if not exists 所以可以直接执行
    for (index in allAddIndexes) {
      Log.I(index)
      this.dber.ExecSQL(index)
    }

    if (includeMigrations) {
      // 4、对比所有的version  找出 migrator
      val oldVersion = this.OldVersion(name)
      val allMigrations = emptyList<MInfo>().toMutableList()
      val nowV = info.Version

      if (nowV != oldVersion) {
        val res = FindBestMigratorPath(oldVersion, nowV, info.Migrators)
        allMigrations += MInfo(res, name, oldVersion, nowV)
      }
      // 执行migrator
      for ((ms, n, from, to) in allMigrations) {
        Log.I("migrate $n from $from to $to ...")
        for (mig in ms) {
          mig.Exe(this) {}
        }
        // 更新版本号，即使每一个 mig 都做了更新，这里也再统一做一次，防止漏做
        // 在每一个mig的执行中，本也应该做一次更好，即使没做，也不影响最后的结果，也不会在下一次重新做
        // 因为这里是事务，所以不用考虑中间失败了，而没有及时做版本号更新的情况。
        this.SetVersion(name, to)
      }
    }

    this.dber.SetTransactionSuccessful()

  } catch (e: Exception) {
    e.printStackTrace();
    throw e
  } finally {
    this.dber.EndTransaction()
  }
}

