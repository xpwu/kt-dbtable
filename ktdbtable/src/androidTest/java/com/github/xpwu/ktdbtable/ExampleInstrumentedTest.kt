package com.github.xpwu.ktdbtable

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.github.xpwu.ktdbtable.test", appContext.packageName)
  }

  val appContext = InstrumentationRegistry.getInstrumentation().targetContext
  val sql = SQLiteDatabase.openOrCreateDatabase(appContext.cacheDir.absolutePath + "/test.db", null)
  val db = DB(SQLiteAdapter(sql))

  @Test
  fun sqlite() {
    val name = User.TableNameIn(db)
    val cursor = db.UnderlyingDB.query(name, null, "${User.Id}=?", arrayOf("0xwew3"), null, null, null)
    while (cursor.moveToNext()) {
      assertEquals( "0xwew3", cursor.getString(0))
      assertEquals( "xp", cursor.getString(1))
      assertEquals( 232323, cursor.getInt(2))
      assertEquals( 4, cursor.getShort(3).toInt())
      assertEquals( "add", cursor.getColumnName(3))
    }
  }

  @Test
  fun query() {
    val table = User.TableNameIn(db)
    val where = User.Id.eq("0xwew3")
    val cursor = db.UnderlyingDB.query(table, where)
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( "0xwew3", user.Id)
      assertEquals( "xp", user.Name)
      assertEquals( 232323, user.Time)
      assertEquals( 4, user.Add.toInt())
    }
  }

//  @Test
//  fun queryKeyword() {
//    val table = User.TableNameIn(db)
//    val where = User.Add.eq(4)
//    val cursor = db.UnderlyingDB.query(table, where)
//    while (cursor.moveToNext()) {
//      val user = User()
//      cursor.ToUser(user)
//      assertEquals( "0xwew3", user.Id)
//      assertEquals( "xp", user.Name)
//      assertEquals( 232323, user.Time)
//      assertEquals( 4, user.Add.toInt())
//    }
//  }

//  @Test
//  fun deleteColumn() {
//    val table = User.TableNameIn(db)
//    //
//    val value = ContentValues(5)
//    value.put(User.Id.toString(), "testDeleteColumn")
//    value.put(User.Time.toString(), )
//    db.UnderlyingDB.insertWithOnConflict(table, null, value.escape(), CONFLICT_REPLACE)
//  }

  @Test
  fun coroutine() = runBlocking {
    val dbQueue = DBQueue {
      return@DBQueue SQLiteAdapter(sql)
    }

    val ret: String = dbQueue {
      val name = User.TableNameIn(it)
      val cursor = it.UnderlyingDB.query(name, null, "${User.Id}=?", arrayOf("0xwew3"), null, null, null)
      var r = ""
      while (cursor.moveToNext()) {
        r = cursor.getString(0)
        break
      }

      r
    }

    assertEquals(ret, "0xwew3")
  }

  @Test
  fun nestQueue() = runBlocking {
    val dbQueue = DBQueue {
      return@DBQueue SQLiteAdapter(sql)
    }

    val ret: String = dbQueue {
      dbQueue {
        val name = User.TableNameIn(it)
        val cursor = it.UnderlyingDB.query(name, null, "${User.Id}=?", arrayOf("0xwew3"), null, null, null)
        var r = ""
        while (cursor.moveToNext()) {
          r = cursor.getString(0)
          break
        }

        r
      }
    }

    assertEquals(ret, "0xwew3")
  }
}

@Table("user")
class User {
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

fun NewUser(id: String, name: String, time: Int): User {
  val u = User()
  u.Id = id
  u.Name = name
  u.Time = time

  return u
}

fun User.Companion.Initializer(): Collection<User> {

  return listOf(User(), NewUser("0x111", "wu", 478))
}