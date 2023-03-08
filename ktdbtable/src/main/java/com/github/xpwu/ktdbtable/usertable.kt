package com.github.xpwu.ktdbtable

fun User.Companion.TableNameIn(db: DB<*>): String {
  val name = db.Name(User::class)?: userTableName

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

val User.Companion.In
  get() = Column("id")

val User.Companion.Id
  get() = Column("id")

val User.Companion.Name
  get() = Column("Name")


fun User.Companion.CreateTableIn(db: DB<*>) {
  db.OnlyForInitTable(listOf(
    "CREATE TABLE IF NOT EXISTS xxx ",
    // create index
    // set VERSION
    // INSERT DATA
  ))
}

private const val userTableName = "user"
private const val userTableVersion = 0






