@file:JvmName("jvmNameTest")
package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table

@Table("user")
class User {
  companion object

  @Column("id")
  var Id: String? = null
  @Column("name", primaryKey = PrimaryKey.MULTI_DESC)
  var Name: String = "xp"
}

fun User.Companion.Initializer(): Collection<User> {
  return emptyList()
}

@Table("nameinfo")
class NameInfo {
  companion object

  @Column("id")
  @Index
  var Id: String? = null
  @Index(true)
  @Index
  @Index(true, "test_index_name")
  @Column("name", primaryKey = PrimaryKey.MULTI_DESC)
  var Name: String = "xp"
}

fun NameInfo.Companion.Initializer(): Collection<NameInfo> {
  return emptyList()
}
