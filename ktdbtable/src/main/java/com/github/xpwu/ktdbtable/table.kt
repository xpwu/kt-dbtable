package com.github.xpwu.ktdbtable

import androidx.collection.SparseArrayCompat
import kotlin.reflect.KClass
import kotlin.reflect.full.*



interface Table {
  fun SqlNameIn(db: DB<*>): String {
    return "`${OriginNameIn(db)}`"
  }

  fun OriginNameIn(db: DB<*>): String
}

// todo: may be error
fun CreateTableIn(table: KClass<*>, db: DB<*>) {
  val eFuncs = table.companionObject?.declaredMemberExtensionFunctions ?: return
  for (func in eFuncs) {
    // extension and para db
    if (func.name == "CreateTableIn" && func.parameters.size == 2) {
      func.call(table.companionObjectInstance, db)
      break
    }
  }
}

data class Version(val from: Int = 0, val to: Int = 0)
//typealias Migration = (DB<*>) -> Unit

interface Migration {
  fun Count(db: DB<*>): Int
  fun Exe(db: DB<*>, progression: (Int)->Unit)
}

fun Migration(block: (DB<*>) -> Unit): Migration {
  return object: Migration {
    override fun Count(db: DB<*>): Int {
      return 1
    }

    override fun Exe(db: DB<*>, progression: (Int) -> Unit) {
      block(db)
      progression(1)
    }
  }
}

typealias ColumnName = String
// "ALTER TABLE table_name ADD COLUMN ..."
typealias AlterSQL = String

typealias IndexName = String
// "CREATE INDEX IF NOT EXISTS ..."
typealias IndexSQL = String

data class TableInfo (
  val Version: Int = 0,
  val Migrators: Map<Version, Migration> = emptyMap(),
  val Indexes: Map<IndexName, IndexSQL> = emptyMap(),
  val Columns: Map<ColumnName, AlterSQL> = emptyMap(),
  val DefaultName: String = ""
)

/**
 * 使用反射方式访问得原因：
 * 1、如果使用生成一个数组的方式，在依赖库的情况下，processor 会对每个依赖库生成一个数组，无法满足通过名字/类名查找所有
 *    Info的要求
 * 2、库的新使用方式也不需要一个全集的 Info 对应关系，且全集的Info 也不能完全的对应好 name=>info
 *
 */
fun GetTableInfo(table: KClass<*>): TableInfo {
  // table 在编译时，processor 会检查 companionObject, 所以一定有 companionObject
  // processor 生成的扩展方法的文件有注解：@file:JvmName("XXXTable")，通过 java 类的 getMethod 能获取到 xxx.TableInfo() 方法
  // processor 生成的 TableInfo 方法, 所以一定存在
  val func = Class.forName(table.qualifiedName + "Table").getMethod("TableInfo", table.companionObject!!.java)
  return func.invoke(null, table.companionObjectInstance) as TableInfo
}

/**
 *
 *   此注释不删除的原因：对类名、表名等的解释仍有意义
 *
 * 记录所有能从 name(table 的默认name) => TableInfo 的映射对 以及 kclass.qualifiedName => TableInfo 的映射对
 * 如果多个 TableInfo 对应同一个 name，不会用name收集在此处，而只是有 kclass.qualifiedName => TableInfo,
 * 并且这种情况不能算是错误，因为：虽然名字相同，但是不一定会在同一个db中，所以不能算错。
 * 这种情况的升级对比放到下次db用到此table时进行。
 *
 * 如果name一开始映射到 TableInfo1，由于代码的修改，可能对应到 TableInfo2，只要唯一对应，就算有效
 * 如果在项目(库)开发阶段，出现在同一个库中的名字冲突，可以在DB初始化 binding table。
 * 项目(库)使用过程中，因为新的table在整个过程中的加入，会造成原来name不冲突的而冲突，但是不会引起在某一个库中的冲突，
 * 如前所述，在table下次被使用时初始化。
 *
 * 在项目(库)使用过程中，table不必也不应该修改已经被使用的名字。
 *
 *
 * 保持 kclass.qualifiedName => TableInfo 的映射是为了加快查找，而不用反射
 */


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

fun FindBestMigratorPath(from: Int, to: Int, m: Map<Version, Migration>): List<Migration> {
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




