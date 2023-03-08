package com.github.xpwu.ktdbtable

import android.database.sqlite.SQLiteDatabase

fun User.Companion.TableNameIn(db: DB<*>): String {
  val name = db.Name(User::class) ?: userTableName

  if (!db.Exist(name)) {
    User.CreateTableIn(db)
  } else {
    db.OnOpenAndUpgrade(name, User::class)
  }

  return name
}

fun User.Companion.Binding(): TableBinding {
  return MakeBinding(User::class, userTableName)
}

val User.Companion.Id
  get() = Column("id")

val User.Companion.Name
  get() = Column("name")


fun User.Companion.CreateTableIn(db: DB<*>) {
  val tableName = db.Name(User::class) ?: userTableName
  db.OnlyForInitTable {
    it.BeginTransaction()
    try {
//      it.ExecSQL("CREATE TABLE IF NOT EXISTS $tableName($columnname $type $primarykey $DESC $AUTOINCREMENT $notnull $defaultvalue, $primarykeyT$($columnname $DESC, $columnname $DESC $) )")
//      it.ExecSQL("CREATE $UNIQUE INDEX IF NOT EXISTS ${tableName}_$columnname ON ${tableName}($columnname $DESC)")
//      db.SetVersion(tableName, userTableVersion)
//      it.Insert(tableName, SQLiteDatabase.CONFLICT_IGNORE, )
      it.SetTransactionSuccessful()
    } finally {
      it.EndTransaction()
    }
  }
}

private const val userTableName = "user"
private const val userTableVersion = 0






