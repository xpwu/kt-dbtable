package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtable.annotation.*
import com.github.xpwu.ktdbtable.annotation.Column
import com.github.xpwu.ktdbtable.annotation.Table

@Table("name")
class User {
  companion object

  @Column("id")
  @Index
  @Index
  var Id: String = ""
  var Name: String = ""
}

@Migrator(User::class)
fun Migrators(): Map<Version, Migration> {
  return mapOf(
    Version(0, 2) to {table -> table.DB}
  )
}

@Initializer(User::class)
fun Initializer(): Collection<User> {
  return listOf()
}

