package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtable.annotation.*
import com.github.xpwu.ktdbtable.annotation.Column
import com.github.xpwu.ktdbtable.annotation.Table

@Table("user")
class User {
  companion object

  @Column("id")
  @Index
  @Index
  var Id: String? = null
  @Column(primaryKey = PrimaryKey.MULTI_DESC)
  var Name: String = "xp"
}


fun User.Companion.Migrators(): Map<Version, Migration> {
  return mapOf(
    Version(0, 2) to {table -> table.DB}
  )
}


fun User.Companion.Initializer(): Collection<User> {
  return listOf()
}

