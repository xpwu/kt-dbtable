package com.github.xpwu.ktdbtable

//data class TableInfo (
//  val Version: Int = 0,
//  val Migrators: Map<Version, Migration> = emptyMap(),
//  val Columns: Map<ColumnName, AlterSQL> = emptyMap(),
//  val Indexes: Map<IndexName, IndexSQL> = emptyMap(),
//)


class TableContainerImpl : TableContainer {
  override val AllTables: Map<String, TableInfo> by lazy {
    mapOf(
      "$defaultName" to TableInfo(userTableVersion, ),
      (User::class.qualifiedName?:"") to TableInfo()
    )
  }
}