package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.PrimaryKey
import com.github.xpwu.ktdbtble.annotation.Table
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun clazz() {
    assertEquals(Map::class.qualifiedName, "kotlin.collections.Map")
    assertEquals(Version::class.qualifiedName, "com.github.xpwu.ktdbtable.Version")
    assertEquals(Collection::class.qualifiedName, "kotlin.collections.Collection")
  }
}

@Table("user")
class User {
  companion object

  @Column("id")
  @Index
  @Index
  var Id: String? = null
  @Column("name", primaryKey = PrimaryKey.MULTI_DESC)
  var Name: String = "xp"
}