package com.github.xpwu.ktdbtable.where

private interface op {
  val value: String
}

enum class LGOperator(override val value: String) : op {
  GT(">"),
  LT("<"),
}

enum class LGEOperator(override val value: String) : op {
  GT(">"),
  LT("<"),
  EQ("="),
  NEQ("!="),
  GTE(">="),
  LTE("<="),
}

enum class EOperator(override val value: String) : op {
  EQ("="),
  NEQ("!="),
}

enum class Null(override val value: String): op {
  IS(" IS NULL"),
  NOT(" IS NOT NULL")
}

class Compare private constructor(field: String, op: op) : Where {

  constructor (field: String, op: LGEOperator, value: Long) : this(field, op) {
    argSql = value.toString()
  }

  constructor (field: String, op: LGEOperator, value: Byte) : this(field, op, value.toLong())

  constructor (field: String, op: LGEOperator, value: Int) : this(field, op, value.toLong())

  constructor (field: String, op: LGEOperator, value: Short) : this(field, op, value.toLong())

  constructor (field: String, op: LGOperator, value: Double) : this(field, op) {
    argSql = value.toString()
  }

  constructor (field: String, op: LGOperator, value: Float) : this(field, op, value.toDouble())

  constructor (field: String, value: Boolean) : this(field, EOperator.EQ) {
    argSql = if (value) "1" else "0"
  }

  // null or not null
  constructor (field: String, op: Null) : this(field, op as op) {
    argSql = ""
  }

  constructor(field: String, op: EOperator, value: String) : this(field, op) {
    argSql = "?"
    bindArgs.add(value)
  }

  private lateinit var argSql: String

  private val bindArgs = ArrayList<String>(1)

  override val ArgSQL: String = field + op.value + argSql

  override val BindArgs: Array<String> by lazy {
    this.bindArgs.toArray(emptyArray())
  }
}