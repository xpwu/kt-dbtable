package com.github.xpwu.ktdbtable

import androidx.collection.SparseArrayCompat
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


interface Table<out T> {
  companion object

//  val DB: DB<out T>
//  val TableName: String
}

//data class TableBase<out T>(override val TableName: String, override val DB: DB<out T>) : Table<T>

data class Version(val from: Int = 0, val to: Int = 0)
typealias Migration = (DB<*>) -> Unit

typealias ColumnName = String
// "ALTER TABLE table_name ADD COLUMN ..."
typealias AlterSQL = String

typealias IndexName = String
// "CREATE INDEX IF NOT EXISTS ..."
typealias IndexSQL = String

class TableInfo {
  val Version = 0
  val Name = ""
  val Migrators: Map<Version, Migration> = emptyMap()
  val Columns: Map<ColumnName, AlterSQL> = emptyMap()
  val Indexes: Map<IndexName, IndexSQL> = emptyMap()
}

interface TableContainer {
  val AllTables: Map<String, TableInfo>
}

/**
 *
 * 记录所有能从 name => kclass 的映射对
 * 如果多个kclass 对应同一个 name，不会用name收集在此处，而是用kclass.qualifiedName 直接当key, 并且这种情况不能算是错误，
 * 因为：虽然名字相同，但是不一定会在同一个db中，所以不能算错。
 * 这种情况的升级对比放到下次db用到此table时进行。
 * 如果name一开始映射到kclass1，由于代码的修改，可能对应到kclass2，只要唯一对应，就算有效
 * 如果在项目(库)开发阶段，出现在同一个库中的名字冲突，可以在DB初始化重命名。
 * 项目(库)使用过程中，因为新的table在整个过程中的加入，会造成原来name不冲突的而冲突，但是不会引起在某一个库中的冲突，
 * 如前所述，在table下次被使用时初始化。
 * 在项目(库)使用过程中，table不必也不应该修改已经被使用的名字。
 *
 */
private val allTables = lazy {
  val kClass = TableContainer::class.qualifiedName
    ?.let { Class.forName(it + "Impl").kotlin }

  (kClass?.createInstance() as? TableContainer)?.AllTables
}

fun Table.Companion.GetInfo(name: String): TableInfo? {
  return allTables.value?.get(name)
}

// todo check path at compile
fun Table.Companion.GetMigrations(name: String, from: Int, to: Int): List<Migration>? {
  return FineBestMigratorPath(from, to, Table.GetInfo(name)?.Migrators)
}

private fun convert(m: Map<Version, Migration>): SparseArrayCompat<SparseArrayCompat<Migration>> {
  val migrations: SparseArrayCompat<SparseArrayCompat<Migration>> = SparseArrayCompat()

  for ((k, v) in m) {
    var f = migrations[k.from]
    if (f == null) {
      f = SparseArrayCompat<Migration>()
      migrations.put(k.from, f)
    }
    if (f[k.to] != null) {
      Log.W("Overriding migration from(%d) to from(%d)", k.from, k.to)
    }
    f.put(k.to, v)
  }

  return migrations
}

fun FineBestMigratorPath(from: Int, to: Int, m: Map<Version, Migration>?): List<Migration>? {
  if (from == to) {
    return emptyList()
  }

  m?:return null

  var start = from
  val migrations = convert(m)

  val result: MutableList<Migration> = ArrayList()

  val upgrade: Boolean = to > start
  val direction = if (upgrade) -1 else 1

  while (if (upgrade) start < to else start > to) {
    val target: SparseArrayCompat<Migration> = migrations.get(start) ?: return emptyList()

    // 因为SparseArrayCompat本身是有序的，所以可以按照顺序找，
    // 这里简单的使用单向最大跨度的方式搜索，而没有采用全局最优方案
    // todo 动态规划全局最优方案
    val size: Int = target.size()
    val firstIndex: Int
    val lastIndex: Int
    if (upgrade) {
      firstIndex = size - 1
      lastIndex = -1
    } else {
      firstIndex = 0
      lastIndex = size
    }
    var found = false
    var i = firstIndex
    while (i != lastIndex) {
      val targetVersion: Int = target.keyAt(i)
      val shouldAddToPath: Boolean = if (upgrade) {
        targetVersion in (start + 1)..to
      } else {
        targetVersion in to until start
      }
      if (shouldAddToPath) {
        result.add(target.valueAt(i))
        start = targetVersion
        found = true
        break
      }
      i += direction
    }
    if (!found) {
      return emptyList()
    }
  }

  return result
}




