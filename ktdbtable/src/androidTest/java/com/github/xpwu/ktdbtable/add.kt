package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table

// add is a key word for sqlite
@Table("add")
class Adder {
  companion object

  @Column("id", primaryKey = PrimaryKey.ONLY_ONE)
  var Id: String = "0xwew3"
  @Column("name")
  var Name: String = "xp"

  @Column("time")
  @Index(unique = true)
  var Time: Int = 232323

  // add  是一个数据库的关键字，此项测试关键字作为列名是否正确
  @Column("add")
  var Add: Short = 4

  @Column("ext")
  var Ext: ByteArray = ByteArray(0)
}

fun NewAdder(id: String, name: String, time: Int): Adder {
  val u = Adder()
  u.Id = id
  u.Name = name
  u.Time = time

  return u
}

fun Adder.Companion.Initializer(): Collection<Adder> {

  return listOf(Adder(), NewAdder("0x111", "wu", 478))
}