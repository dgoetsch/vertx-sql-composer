package dev.yn.playground.ex

import dev.yn.playground.sql.*
import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.Task
import dev.yn.playground.andThen

fun <C: SQLTaskContext<*>> Task<Unit, Unit, C>.dropTableIfExists(tableName: String) =
        this.andThen(SQLTask.exec(SQLMapping.Companion.exec("DROP TABLE IF EXISTS $tableName")))

fun dropTableIfExists(tableName: String) =
        SQLTask.exec(SQLMapping.Companion.exec("DROP TABLE IF EXISTS $tableName"))

fun <C: SQLTaskContext<*>> Task<Unit, Unit, C>.deleteAll(tableName: String) =
        this.andThen(dev.yn.playground.sql.SQLTask.exec(dev.yn.playground.sql.SQLMapping.Companion.exec("DELETE FROM $tableName")))

fun deleteAll(tableName: String) =
        SQLTask.exec(SQLMapping.Companion.exec("DELETE FROM $tableName"))

fun <I, O, J, C: SQLTaskContext<*>> Task<I, O, C>.query(mapping: SQLQueryMapping<O, J>) =
        this.andThen(SQLTask.query(mapping))

fun <I, O, J, C: SQLTaskContext<*>> Task<I, O, C>.update(mapping: SQLUpdateMapping<O, J>) =
        this.andThen(SQLTask.update(mapping))

fun <I, O, J, C: SQLTaskContext<*>> Task<I, O, C>.exec(mapping: SQLExecMapping<O, J>) =
        this.andThen(SQLTask.exec(mapping))