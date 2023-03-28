package com.github.xpwu.ktdbtable

import com.github.xpwu.ktdbtble.annotation.*
import com.github.xpwu.ktdbtble.annotation.Table
import java.math.BigInteger

@Table("TestFromTo")
class TestFromTo {
  companion object

  @Column("i")
  var Integer0: Int = 0

  @Column("i2")
  var Blob: ByteArray = byteArrayOf()

  @Column("fromto")
  var From: BigInteger = BigInteger.ZERO
}

@FromByteArray
fun FromByteArray(byteArray: ByteArray): BigInteger {
  return BigInteger(byteArray)
}

@ToByteArray
fun ToByteArray1(big: BigInteger): ByteArray {
  return big.toByteArray()
}

