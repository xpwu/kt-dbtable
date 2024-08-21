package com.github.xpwu.ktdbtable.where

import com.github.xpwu.ktdbtable.escape


interface Where {
  val ArgSQL: String
  val BindArgs: Array<String>
}

val Where.RawSQL: String
  get() {
    val bind = this.BindArgs.map { "'" + (it.escape()) + "'" }
    val ba: Array<String> = Array<String>(bind.size) { "" }
    for (i in bind.indices) {
      ba[i] = bind[i]
    }

    return String.format(this.ArgSQL.replace("?", "%s"), *ba)
  }

infix fun Where.and(where: Where): Where {
  return And(arrayOf(this, where))
}

infix fun Where.or(where: Where): Where {
  return Or(arrayOf(this, where))
}

val Where.not: Where
  get() = Not(this)


