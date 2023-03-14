package com.github.xpwu.ktdbtable.example

import android.database.sqlite.SQLiteDatabase
import com.github.xpwu.ktdbtable.DB
import com.github.xpwu.ktdbtable.Migration
import com.github.xpwu.ktdbtable.Version
import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table

@Table("user")
class User {
  companion object

  @Column("id")
  @Index
  var Id: String? = null
  @Column("name", primaryKey = PrimaryKey.MULTI_DESC)
  var Name: String = "xp"
}


//fun User.Companion.Migrators(): Map<Version, Migration> {
//  return mapOf(
//    Version(0, 2) to {
//        db ->
//        val db = db.UnderlyingDB as? SQLiteDatabase
//        db?.endTransaction()}
//  )
//}

fun User.Companion.Update(db: DB<SQLiteDatabase>) {
  val name = User.TableNameIn(db)
  db.UnderlyingDB.execSQL("")
}


//fun User.Companion.Initializer(): Collection<User> {
//  return emptyList()
//}

