package com.github.xpwu.ktdbtable.where

open class Like(field: String, private val pattern: String) : Where {

  override val ArgSQL: String = "$field LIKE ? "

  override val BindArgs: Array<String>
    get() = arrayOf(pattern)
}

// 模糊搜索
class Fuzzy(field: String, string: String) : Like(field, "%$string%")
