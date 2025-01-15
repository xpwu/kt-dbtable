package com.github.xpwu.ktdbtable

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Updating {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.github.xpwu.ktdbtable.test", appContext.packageName)
  }

  val appContext = InstrumentationRegistry.getInstrumentation().targetContext
  val sql = SQLiteDatabase.openOrCreateDatabase(appContext.cacheDir.absolutePath + "/testupdating.db", null)

  @Test
  fun autoUpdating() {
    var db = DB(SQLiteAdapter(sql))
    // 删除之前的表，建立测试环境
    db.UnderlyingDB.execSQL("DROP TABLE IF EXISTS `userautoupdating`")

    val name = UserOld.asTable().SqlNameIn(db)
    val cursor = db.UnderlyingDB.query(name, null, "${User.Id}=?", arrayOf("0xwew3"), null, null, null)
    while (cursor.moveToNext()) {
      assertEquals(5, cursor.columnCount)
      assertEquals( "0xwew3", cursor.getString(0))
      assertEquals( "xp", cursor.getString(1))
      assertEquals( 232323, cursor.getInt(2))
      assertEquals( 4, cursor.getShort(3).toInt())
      assertEquals( "add", cursor.getColumnName(3))
    }

    // 重新生成 DB 对象，模拟新打开数据库
    db = DB(SQLiteAdapter(sql))
    val name2 = UserForUpdating.asTable().SqlNameIn(db)
    val cursor2 = db.UnderlyingDB.query(name2, null, "${User.Id}=?", arrayOf("0xwew3"), null, null, null)
    while (cursor2.moveToNext()) {
      assertEquals(6, cursor2.columnCount)
      assertEquals( "0xwew3", cursor2.getString(0))
      assertEquals( "xp", cursor2.getString(1))
      assertEquals( 232323, cursor2.getInt(2))
      assertEquals( 4, cursor2.getShort(3).toInt())
      assertEquals( "add", cursor2.getColumnName(3))
    }
  }
}

@Table("userautoupdating")
class UserOld {
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

fun NewUserOld(id: String, name: String, time: Int): UserOld {
  val u = UserOld()
  u.Id = id
  u.Name = name
  u.Time = time

  return u
}

fun UserOld.Companion.Initializer(): Collection<UserOld> {

  return listOf(UserOld(), NewUserOld("0x111", "wu", 478))
}

@Table("userautoupdating")
class UserForUpdating {
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

  @Column("updating")
  var Updating: String = "test updating"
}
