package com.github.xpwu.ktdbtable.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
// version default is 0, do not edit
annotation class Table(val name: String, val version: Int = 0)


/**
 *
ALTER:

https://www.sqlite.com/lang_altertable.html

The new column may take any of the forms permissible in a CREATE TABLE statement, with the following restrictions:

1、The column may not have a PRIMARY KEY or UNIQUE constraint.
2、The column may not have a default value of CURRENT_TIME, CURRENT_DATE, CURRENT_TIMESTAMP, or an expression in parentheses.
3、If a NOT NULL constraint is specified, then the column must have a default value other than NULL.
4、If foreign key constraints are enabled and a column with a REFERENCES clause is added, the column must have a default value of NULL.
5、The column may not be GENERATED ALWAYS ... STORED, though VIRTUAL columns are allowed.

 */

/**
 *
 * TYPE 根据字段定义自动推断
 *
 * Byte, Short, Int, Long  => INTEGER
 * Boolean => INTEGER ( true -> 1 ; false -> 0 )
 * String => TEXT
 * Float, Double => REAL
 * ByteArray, Array<Byte> => BLOB
 * 其他类型会报错  todo 支持自定义的转换函数
 *
 * DEFAULT 通过属性定义时的默认值自动获取
 *
 * UNIQUE 放入Index注解中，也方便 ALTER 的使用
 *
 * CHECK 约束：CHECK 约束确保某列中的所有值满足一定条件, 暂未支持 // todo
 *
 * @param name: 如果为"_"，则忽略此字段
 *
 *   https://www.sqlite.org/lang_createtable.html
 * @param primaryKey PRIMARY Key 约束，同时也必须是NOT NULL，字段不能是 option 的。MULTI_约束的顺序与sequence定义的顺序一致
 *
 * @param notNull NOT NULL, 如果属性是 option 的定义，notNull必须为false，即使设定notNull为true，该设置也不生效。
 *
 * @param sequence PRIMARY KEY 约束的顺序，只要相对有大小比较就行，并不严格要求数字是连续的
 *
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Column(
  // 为了防止修改属性名，而造成列名非预期的自动修改，name在注解中必须明确赋值
  val name: String,
  val primaryKey: PrimaryKey = PrimaryKey.FALSE,
  val notNull: Boolean = false,
  val sequence:Int = 0
)

// todo   AUTOINCREMENT 只能与int 结合使用

enum class PrimaryKey(val result:String) {
  FALSE(""), MULTI(""), MULTI_DESC("DESC"),
  ONLY_ONE("PRIMARY KEY NOT NULL"), ONLY_ONE_DESC("PRIMARY KEY DESC NOT NULL"),
  ONLY_ONE_AUTO_INC("PRIMARY KEY AUTOINCREMENT NOT NULL"),
  ONLY_ONE_AUTO_INC_DESC("PRIMARY KEY DESC AUTOINCREMENT NOT NULL"),
}

/**
 *
 *
 * @param name: 1、如果设置为""，则为 tableName_columnName;
 *              2、如果不是默认值，name相同的一起形成联合索引，联合索引的名字为tableName_name
 *              3、联合索引的顺序与sequence定义的顺序一致。
 *
 * @param unique
 *
 * @param desc DESC
 *
 * @param sequence 联合索引的顺序，只要相对有大小比较就行，并不严格要求数字是连续的
 *
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class Index(val unique:Boolean = false,
                       val name: String = "",
                       val desc:Boolean = false,
                       val sequence:Int = 0)
