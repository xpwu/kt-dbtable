package com.github.xpwu.ktdbtable.annotation

import kotlin.reflect.KClass

annotation class Table(val name: String, val version: Int = 0)

annotation class Column(val name: String = "")

annotation class Migrator(val clazz: KClass<*>)

/**
 *
 */
annotation class Initializer(val clazz: KClass<*>)

@Repeatable
annotation class Index(val name: String = "")
