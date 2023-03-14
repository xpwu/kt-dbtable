package com.github.xpwu.ktdbtable.example.out

import android.content.ContentValues
import com.github.xpwu.ktdbtable.*
import com.github.xpwu.ktdbtable.example.Migrators
import com.github.xpwu.ktdbtable.example.User

fun User.Companion.TableNameIn(db: DB<*>): String {
  val name = db.Name(User::class) ?: tableName

  if (!db.Exist(name)) {
    User.CreateTableIn(db)
  } else {
    db.OnOpenAndUpgrade(name, User::class)
  }

  return name
}

val User.Companion.Id
  get() = Column("id")

val User.Companion.Name
  get() = Column("name")

fun User.Companion.TableInfo(): TableInfo {
  return TableInfo(tableVersion, User.Migrators())
}

fun User.Companion.Binding(): TableBinding {
  return MakeBinding(User::class, tableName)
}

private fun User.Companion.CreateTableIn(db: DB<*>) {
  val tableName = db.Name(User::class) ?: tableName
  db.OnlyForInitTable {
    it.BeginTransaction()
    try {
      it.ExecSQL("CREATE TABLE IF NOT EXISTS xxx")
      it.ExecSQL("CREATE xxx INDEX IF NOT EXISTS xxx")
      db.SetVersion(tableName, tableVersion)
      it.Replace(tableName, User().ToContentValues())
      it.SetTransactionSuccessful()
    } finally {
      it.EndTransaction()
    }
  }
}

fun User.Companion.AllColumns(): List<Column> {
  return listOf(
    Id, Name,
  )
}

fun User.ToContentValues(columns: List<Column> = User.AllColumns()): ContentValues {
  val cv = ContentValues(columns.size)
  for (column in columns) {
    when(column) {
      User.Id -> cv.put(column.toString(), this.Id)
      User.Name -> cv.put(column.toString(), this.Name)
      else -> {
        throw IllegalArgumentException("Illegal column $column for User")
      }
    }
  }

  return cv
}

private const val tableName = "user"
private const val tableVersion = 0
