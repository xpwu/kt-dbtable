package com.github.xpwu.ktdbtable

fun <T> User.Companion.TableName(db: DB<*>): String {
  if (!db.Exist(userTableName)) {
    User.CreateTableIn(db)
  }
  return userTableName
}

val User.Companion.In
  get() = Column("id")

val User.Companion.Id
  get() = Column("id")

val User.Companion.Name
  get() = Column("Name")


private fun User.Companion.CreateTableIn(db: DB<*>) {
  db.OnlyForInitTable(listOf(
    "CREATE TABLE IF NOT EXISTS xxx ",
    // create index
    // set VERSION
    // INSERT DATA
  ))
}

private const val userTableName = "user"
private const val userTableVersion = 0






