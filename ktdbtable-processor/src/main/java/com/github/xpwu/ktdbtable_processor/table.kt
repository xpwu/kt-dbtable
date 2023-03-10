package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class TableInfo(
  val Name: String,
  val Version: Int,
  val Type: TypeElement
) {
  val Test: String = ""
  var TestInt: Int = 9
  var TestUInt: UInt = 9U
  val ByteA: ByteArray = ByteArray(0)
  val UByteA: UByteArray = UByteArray(0)
  val TestArray: Array<Byte> = arrayOf()
  var TestArrayNull: Array<Byte>? = arrayOf()
}

enum class Type {
  TEXT,
  INTEGER,
  REAL,
  BLOB
}

/**
 *
 * Byte, Short, Int, Long (Uxxx)  => INTEGER
 * Boolean => INTEGER ( true -> 1 ; false -> 0 )
 * String => TEXT
 * Float, Double => REAL
 * ByteArray, Array<Byte> (UByte) => BLOB
 *
 */

fun entity2column(type: TypeMirror): Type? {
  if (type.kind.isPrimitive) {
    return primitive2column[type.kind]
  }

  return complex2column[type.toString()]
}

val primitive2column = mapOf<TypeKind, Type>(
  TypeKind.BYTE to Type.INTEGER,
  TypeKind.SHORT to Type.INTEGER,
  TypeKind.INT to Type.INTEGER,
  TypeKind.LONG to Type.INTEGER,
  TypeKind.BOOLEAN to Type.INTEGER,
  TypeKind.FLOAT to Type.REAL,
  TypeKind.DOUBLE to Type.REAL,
)

@OptIn(ExperimentalUnsignedTypes::class)
val complex2column = mapOf<String, Type>(
  String::class.java.canonicalName to Type.TEXT,
  ByteArray::class.java.canonicalName to Type.BLOB,
  Array<Byte>::class.java.canonicalName to Type.BLOB,
  UByteArray::class.java.canonicalName to Type.BLOB,
  Array<UByte>::class.java.canonicalName to Type.BLOB,
)

class ColumnInfo(
  val Typ: Type,
  val FieldName: String,
  val ColumnAnno: Column,
  val IndexAnnotations: ArrayList<Index>,
)
