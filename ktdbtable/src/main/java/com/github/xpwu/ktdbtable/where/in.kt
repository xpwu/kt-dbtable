package com.github.xpwu.ktdbtable.where

class In private constructor( field: String) : Where {
  constructor (field: String, values: LongArray) : this(field) {
    argSql = values.joinToString(",", "(", ")")
  }

  constructor (field: String, values: ByteArray) : this(field) {
    argSql = values.joinToString(",", "(", ")")
  }

  constructor (field: String, values: IntArray) : this(field) {
    argSql = values.joinToString(",", "(", ")")
  }

  constructor (field: String, values: BooleanArray) : this(field) {
    argSql = values.joinToString(",", "(", ")") { if (it) "1" else "0"}
  }

  constructor(field: String, values: Array<String>) : this(field) {
    argSql = values.joinToString(",", "(", ")") { "?" }
    bindArgs = values
  }

  private lateinit var argSql: String

  private var bindArgs = emptyArray<String>()

  override val ArgSQL: String = "$field IN $argSql"

  override val BindArgs: Array<String>
    get() = bindArgs

}