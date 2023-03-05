package com.github.xpwu.ktdbtable

fun User.Companion.In(db: DB): Table {
  if (!db.Exist(UserTableName)) {
    User.CreateTable(db)
  }
  return TableBase(UserTableName, db)
}

val User.Companion.Id
  get() = Column("id")

val User.Companion.Name
  get() = Column("Name")


private fun User.Companion.CreateTable(db: DB) {

  db.execSQL("CREATE TABLE IF NOT EXISTS xxx ")
  // create index
  db.execSQL("")
  // VERSION
  db.execSQL("")
  // INSERT DATA
  db.execSQL("")

}

private const val UserTableName = "user"
private const val UserTableVersion = 0






