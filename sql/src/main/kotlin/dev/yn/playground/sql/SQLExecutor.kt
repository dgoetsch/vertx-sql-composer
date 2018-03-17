package dev.yn.playground.sql

import dev.yn.playground.result.AsyncResult

abstract class SQLExecutor {
    abstract fun <T> close(): (T) -> AsyncResult<T>
    abstract fun <T> closeOnFailure(): (Throwable) -> AsyncResult<T>
    abstract fun <T> commit(): (T) -> AsyncResult<T>
    abstract fun <T> rollback(): (Throwable) -> AsyncResult<T>
    abstract fun setAutoCommit(autoCommit: Boolean): AsyncResult<Unit>

    abstract fun query(query: SQLCommand.Query): AsyncResult<SQLResult.Query>
    abstract fun update(update: SQLCommand.Update): AsyncResult<SQLResult.Update>
    abstract fun exec(exec: SQLCommand.Exec): AsyncResult<SQLResult.Void>
}