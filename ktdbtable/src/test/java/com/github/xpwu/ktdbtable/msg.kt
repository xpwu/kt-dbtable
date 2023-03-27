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

  @Column("no")
  var No: Int? = 0
  @Column("no00")
  var No00: Int = 0
  @Column("no1")
  var No1: Short? = 0
  @Column("no11")
  var No11: Short = 0
  @Column("no2")
  var No2: Byte? = 0
  @Column("no22")
  var No22: Byte = 0
  @Column("no3")
  var No3: Double? = 0.0
  @Column("no33")
  var No33: Double = 0.0
  @Column("no4")
  var No4: Float? = 0F
  @Column("no44")
  var No44: Float = 0F
  @Column("no5")
  var No5: Long? = 0L
  @Column("no55")
  var No55: Long = 0L
  @Column("no6")
  var No6: Boolean? = false
  @Column("no66")
  var No66: Boolean = false

  @Column("nos")
  var NoS: String? = null

  @Column("noba")
  var NoBA: ByteArray? = null
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
