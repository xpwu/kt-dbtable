package com.github.xpwu.ktdbtable

import androidx.collection.SparseArrayCompat
import kotlin.reflect.full.createInstance


interface Table {
  val DB: DB
  val Name: String
}

class TableBase(override val Name: String, override val DB: DB) : Table

data class Version(val from: Int = 0, val to: Int = 0)
typealias Migration = (Table) -> Unit

typealias ColumnName = String
typealias AlterSQL = String

typealias IndexName = String
typealias IndexSQL = String

class TableInfo {
  val Version = 0
  val Name = ""
  val Migrator: Map<Version, Migration> = emptyMap()
  val Columns: Map<ColumnName, AlterSQL> = emptyMap()
  val Indexes: Map<IndexName, IndexSQL> = emptyMap()
}

interface TableContainer {
  val AllTables: Map<String, TableInfo>
}

class TableContainer_Impl : TableContainer {
  override val AllTables: Map<String, TableInfo> by lazy {
    mapOf(
      // todo
    )
  }
}

fun GetTableInfo(name: String): TableInfo? {
  val kClass = TableContainer::class.qualifiedName
    ?.let { Class.forName(it + "_Impl").kotlin }

  return (kClass?.createInstance() as? TableContainer)?.AllTables?.get(name)
}

//fun GetTableLatestVersion(name: String): Int? {
//  return GetTableInfo(name)?.Version
//}

fun GetTableMigrators(name: String, from: Int, to: Int): List<Migration> {
  return FineBestMigratorPath(from, to, GetTableInfo(name)?.Migrator ?: return emptyList())
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

fun FineBestMigratorPath(from: Int, to: Int, m: Map<Version, Migration>): List<Migration> {
  if (from == to) {
    return emptyList()
  }

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




