package dev.yn.playground.sql

import org.funktionale.tries.Try
import java.time.Instant

sealed class SQLResult {
    data class Query(val result: Iterator<SQLRow>): SQLResult()
    data class Update(val count: Int): SQLResult()
    object Void: SQLResult()
    class Generic<T>(val result: T): SQLResult()
}

interface SQLRow {
    fun getString(key: String): String
    fun getBoolean(key: String): Boolean
    fun getLong(key: String): Long
    fun getInt(key: String): Int
    fun getInstant(key: String): Instant
}

sealed class SQLCommand {
    data class Query(val statement: SQLStatement) : SQLCommand()
    data class Update(val statement: SQLStatement) : SQLCommand()
    data class Exec(val statement: SQLStatement) : SQLCommand()
}

sealed class SQLStatement {
    data class Parameterized(val query: String, val params: List<Any>): SQLStatement()
    data class Simple(val query: String): SQLStatement()
}

typealias SQLUpdateMapping<I, O> = SQLMapping<I, O, SQLCommand.Update, SQLResult.Update>
typealias SQLQueryMapping<I, O> = SQLMapping<I, O, SQLCommand.Query, SQLResult.Query>
typealias SQLExecMapping<I, O> = SQLMapping<I, O, SQLCommand.Exec, SQLResult.Void>

interface SQLMapping<I, O, C: SQLCommand, R: SQLResult> {
    fun toSql(i: I): C
    fun mapResult(i: I, rs: R): Try<O>

    companion object {
        fun <I, O, C: SQLCommand, R: SQLResult> new(toSql: (I) -> C, mapResult: (I, R) -> Try<O>): SQLMapping<I, O, C, R> =
                object: SQLMapping<I, O, C, R> {
                    override fun toSql(i: I): C = toSql(i)
                    override fun mapResult(i: I, rs: R): Try<O> = mapResult(i, rs)
                }

        fun <I, O> query(toSql: (I) -> SQLCommand.Query, mapResult: (I, SQLResult.Query) -> Try<O>): SQLMapping<I, O, SQLCommand.Query, SQLResult.Query> =
                object: SQLMapping<I, O, SQLCommand.Query, SQLResult.Query> {
                    override fun toSql(i: I): SQLCommand.Query = toSql(i)
                    override fun mapResult(i: I, rs: SQLResult.Query): Try<O> = mapResult(i, rs)
                }

        fun <I, O> update(toSql: (I) -> SQLCommand.Update, mapResult: (I, SQLResult.Update) -> Try<O>): SQLMapping<I, O, SQLCommand.Update, SQLResult.Update> =
                object: SQLMapping<I, O, SQLCommand.Update, SQLResult.Update> {
                    override fun toSql(i: I): SQLCommand.Update = toSql(i)
                    override fun mapResult(i: I, rs: SQLResult.Update): Try<O> = mapResult(i, rs)
                }

        fun <I, O> exec(toSql: (I) -> SQLCommand.Exec, mapResult: (I, SQLResult.Void) -> Try<O>): SQLMapping<I, O, SQLCommand.Exec, SQLResult.Void> =
                object: SQLMapping<I, O, SQLCommand.Exec, SQLResult.Void> {
                    override fun toSql(i: I): SQLCommand.Exec = toSql(i)
                    override fun mapResult(i: I, rs: SQLResult.Void): Try<O> = mapResult(i, rs)
                }

        fun exec(command: String): SQLMapping<Unit, Unit, SQLCommand.Exec, SQLResult.Void> = exec<Unit, Unit>(
                { SQLCommand.Exec(SQLStatement.Simple(command)) },
                { unit, result -> Try.Success(unit) })

    }
}

