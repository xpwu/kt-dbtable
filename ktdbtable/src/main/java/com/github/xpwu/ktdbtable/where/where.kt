package com.github.xpwu.ktdbtable.where


interface Where {
  val ArgSQL: String
  val BindArgs: Array<String>
}

fun String.escape(): String {
  return this.replace("'", "''")
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

