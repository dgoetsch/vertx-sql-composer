package dev.yn.playground.sql

import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.Task
import dev.yn.playground.andThen
import dev.yn.playground.result.AsyncResult
import org.funktionale.tries.Try

fun <I, O, O2, C: SQLTaskContext<*>> Task<I, O, C>.query(mapping: SQLMapping<O, O2, SQLCommand.Query, SQLResult.Query>): Task<I, O2, C> =
        this.andThen(SQLTask.query(mapping))

fun <I, O, O2, C: SQLTaskContext<*>> Task<I, O, C>.update(mapping: SQLMapping<O, O2, SQLCommand.Update, SQLResult.Update>): Task<I, O2, C> =
        this.andThen(SQLTask.update(mapping))

fun <I, O, O2, C: SQLTaskContext<*>> Task<I, O, C>.exec(mapping: SQLMapping<O, O2, SQLCommand.Exec, SQLResult.Void>): Task<I, O2, C> =
        this.andThen(SQLTask.exec(mapping))

sealed class SQLTask<IN, OUT>: Task<IN, OUT, SQLTaskContext<*>> {

    companion object {
        fun <IN, OUT> query(mapping: SQLMapping<IN, OUT, SQLCommand.Query, SQLResult.Query>): SQLTask<IN, OUT> = Query(mapping)
        fun <IN, OUT> update(mapping: SQLMapping<IN, OUT, SQLCommand.Update, SQLResult.Update>): SQLTask<IN, OUT> = Update(mapping)
        fun <IN, OUT> exec(mapping: SQLMapping<IN, OUT, SQLCommand.Exec, SQLResult.Void>): SQLTask<IN, OUT> = Exec(mapping)
    }


    abstract val mapping: SQLMapping<IN, OUT, *, *>

    private data class Query<IN, OUT>(override val mapping: SQLMapping<IN, OUT, SQLCommand.Query, SQLResult.Query>): SQLTask<IN, OUT>() {
        override fun run(i: IN, context: SQLTaskContext<*>): AsyncResult<OUT> {
            val sqlCommand = mapping.toSql(i)
            return context.getSQLExecutor().query(sqlCommand)
                    .map { mapping.mapResult(i, it) }
                    .flatMap(this::handleFailure)
                    .recover { AsyncResult.failed(SQLError.OnCommand(sqlCommand, it)) }
        }
    }

    private data class Update<IN, OUT>(override val mapping: SQLMapping<IN, OUT, SQLCommand.Update, SQLResult.Update>): SQLTask<IN, OUT>() {
        override fun run(i: IN, context: SQLTaskContext<*>): AsyncResult<OUT> {
            val sqlCommand = mapping.toSql(i)
            return context.getSQLExecutor().update(sqlCommand)
                    .map { mapping.mapResult(i, it) }
                    .flatMap(this::handleFailure)
                    .recover { AsyncResult.failed(SQLError.OnCommand(sqlCommand, it)) }
        }
    }

    private data class Exec<IN, OUT>(override val mapping: SQLMapping<IN, OUT, SQLCommand.Exec, SQLResult.Void>): SQLTask<IN, OUT>() {
        override fun run(i: IN, context: SQLTaskContext<*>): AsyncResult<OUT> {
            val sqlCommand = mapping.toSql(i)
            return context.getSQLExecutor().exec(sqlCommand)
                    .map { mapping.mapResult(i, it) }
                    .flatMap(this::handleFailure)
                    .recover { AsyncResult.failed(SQLError.OnCommand(sqlCommand, it)) }
        }
    }

    protected fun <R> handleFailure(tri: Try<R>): AsyncResult<R> =
            when(tri) {
                is Try.Failure -> AsyncResult.failed(tri.throwable)
                is Try.Success -> AsyncResult.succeeded(tri.get())
            }
}





