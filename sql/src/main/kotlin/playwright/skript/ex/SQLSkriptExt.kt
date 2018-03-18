package playwright.skript.ex

import playwright.skript.Skript
import playwright.skript.sql.*
import playwright.skript.stage.SQLCast

fun <Stage> Skript<Unit, Unit, Stage>.dropTableIfExists(tableName: String) where Stage: SQLCast =
        this.andThen(SQLSkript.exec(SQLMapping.exec("DROP TABLE IF EXISTS $tableName")))

fun dropTableIfExists(tableName: String): SQLSkript<Unit, Unit> =
        SQLSkript.exec(SQLMapping.exec("DROP TABLE IF EXISTS $tableName"))

fun <Stage> Skript<Unit, Unit, Stage>.deleteAll(tableName: String): Skript<Unit, Unit, Stage> where Stage: SQLCast =
        this.andThen(SQLSkript.exec(SQLMapping.exec("DELETE FROM $tableName")))

fun deleteAll(tableName: String): SQLSkript<Unit, Unit> =
        SQLSkript.exec(SQLMapping.exec("DELETE FROM $tableName"))

fun <I, O, J, Stage> Skript<I, O, Stage>.query(mapping: SQLQueryMapping<O, J>) where Stage: SQLCast =
        this.andThen(SQLSkript.query(mapping))

fun <I, O, J, Stage> Skript<I, O, Stage>.update(mapping: SQLUpdateMapping<O, J>) where Stage: SQLCast =
        this.andThen(SQLSkript.update(mapping))

fun <I, O, J, Stage> Skript<I, O, Stage>.exec(mapping: SQLExecMapping<O, J>) where Stage: SQLCast =
        this.andThen(SQLSkript.exec(mapping))