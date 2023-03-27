package com.github.xpwu.ktdbtable

import android.database.sqlite.SQLiteDatabase
import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table

@Table("msg")
class Msg {
  companion object

  @Column("id")
  var Id: String? = null
  @Column("name")
  var Name: String = "xp"

  @Column("local_no", primaryKey = PrimaryKey.ONLY_ONE_AUTO_INC)
  var LocaleNo: Int = 0
}

fun Msg.Companion.Migrators(): Map<Version, Migration> {
  return mapOf(
    Version(0, 2) to {
        db1 ->
      val db = db1.UnderlyingDB as? SQLiteDatabase
      db?.execSQL("")}
  )
}

fun Msg.Companion.Update(db: DB<SQLiteDatabase>) {
  val name = Msg.TableNameIn(db)
  db.UnderlyingDB.update(name, Msg().ToContentValues(), "", null)
}

@Table("group")
class Group {
  companion object

  @Column("id", primaryKey = PrimaryKey.ONLY_ONE)
  var Id: String = ""
  @Column("name")
  var Name: String = "xp"
  var inner: Int = 0

  @Column("mems")
  val Members: ByteArray = byteArrayOf()

  @Column("mems1")
  val Members1: Int = 10

}

@Table("group")
class GroupInfo {
  companion object

  @Column("id", primaryKey = PrimaryKey.MULTI_DESC)
  var Id: String = ""
  @Column("name")
  var Name: String = "xp"
  var inner: Int = 0

  @Column("mems")
  val Members: ByteArray = byteArrayOf()

  @Column("mems1", primaryKey = PrimaryKey.MULTI)
  val Members1: Int = 10

}
