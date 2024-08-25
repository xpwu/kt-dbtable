package com.github.xpwu.ktdbtable

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
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
    val name = User.asTable().SqlNameIn(db)
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
    val where = User.Id.eq("0xwew3")
    val cursor = db.query(User.asTable(), where)
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( "0xwew3", user.Id)
      assertEquals( "xp", user.Name)
      assertEquals( 232323, user.Time)
      assertEquals( 4, user.Add.toInt())
    }
  }

  @Test
  fun deleteColumn() {
    val where = User.Id.eq("0xwew3")
    val cursor = db.query(User.asTable(), where)
    while (cursor.moveToNext()) {
      val user = UserDelete()
      cursor.ToUserDelete(user)
      assertEquals( "0xwew3", user.Id)
      assertEquals( "xp", user.Name)
      assertEquals( 4, user.Add.toInt())

      // deleted column for UserDelete
      assertEquals( 232323, cursor.getInt(2))
      assertEquals( User.Time.toString(), cursor.getColumnName(2))
      assertEquals( User.Ext.toString(), cursor.getColumnName(4))
    }
  }

  @Test
  fun selectColumns() {
    val where = User.Id.eq("0xwew3")
    val cursor = db.query(User.asTable(), arrayOf(User.Id, User.Time), where)
    cursor.moveToFirst()
    val user = User()
    val has = cursor.ToUser(user)

    assertEquals( "0xwew3", user.Id)
    assertEquals( 232323, user.Time)

    assertEquals(true, has.Id)
    assertEquals(true, has.Time)
    assertEquals(false, has.Name)
    assertEquals(false, has.Add)
    assertEquals(false, has.Ext)
  }

  @Test
  fun insert() {
    val table = User.asTable()
    val user = NewUser("inse-id", "ins", 5)
    db.UnderlyingDB.insertWithOnConflict(table.SqlNameIn(db), null, user.ToContentValues(
      listOf( User.Id, User.Name, User.Time)), CONFLICT_REPLACE)

    val cursor = db.query(table,User.Id eq "inse-id")
    cursor.moveToFirst()
    val user2 = User()
    val has = cursor.ToUser(user2)

    assertEquals( "inse-id", user.Id)
    assertEquals("ins", user.Name)
    assertEquals( 5, user.Time)

    assertEquals(true, has.Id)
    assertEquals(true, has.Time)
    assertEquals(true, has.Name)
    assertEquals(false, has.Add)
    assertEquals(false, has.Ext)
  }

  @Test
  fun queryKeyword() {
    val where = User.Add.eq(4)
    val cursor = db.query(User.asTable(), where)
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( 4, user.Add.toInt())
    }
  }

  @Test
  fun queryWhere() {
    val table = User.asTable()
    var cursor = db.query(table, User.Add eq 4)
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( 4, user.Add.toInt())
    }

    cursor = db.query(table, User.Add gte 3)
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( 4, user.Add.toInt())
    }

    cursor = db.query(table, User.Add btw Pair(3, 5))
    while (cursor.moveToNext()) {
      val user = User()
      cursor.ToUser(user)
      assertEquals( 4, user.Add.toInt())
    }

    cursor = db.query(table, User.Id `in` arrayOf("0x111", "0xwew3"))
    val user = User()
    cursor.moveToNext()
    cursor.ToUser(user)
    assertEquals( "0x111", user.Id)
    cursor.moveToNext()
    cursor.ToUser(user)
    assertEquals( "0xwew3", user.Id)
  }

  @Test
  fun coroutine() = runBlocking {
    val dbQueue = DBQueue {
      return@DBQueue SQLiteAdapter(sql)
    }

    val ret: String = dbQueue {
      val name = User.asTable().SqlNameIn(it)
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
        val name = User.asTable().SqlNameIn(it)
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

  @Test
  fun tableNameKeyword() {
    val cursor = db.query(Adder.asTable(), Adder.Id eq "0x111")
    while (cursor.moveToNext()) {
      val a = Adder()
      cursor.ToAdder(a)
      assertEquals( 4, a.Add.toInt())
    }
  }
}


