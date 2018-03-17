package dev.yn.playground.sql

import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import dev.yn.playground.result.VertxResult
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import java.time.Instant

class VertxSQLExecutor(val connection: SQLConnection): SQLExecutor() {
    override fun query(query: SQLCommand.Query): AsyncResult<SQLResult.Query> {
        val sqlFuture = Future.future<ResultSet>()
        val statement = query.statement
        when (statement) {
            is SQLStatement.Parameterized -> connection.queryWithParams(statement.query, JsonArray(statement.params), sqlFuture.completer())
            is SQLStatement.Simple -> connection.query(statement.query, sqlFuture.completer())
        }
        return VertxResult(sqlFuture)
                .map { SQLResult.Query(VertxRowIterator(it.rows.iterator())) }
                .recover { CompletableResult.failed(SQLError.OnCommand(query, it)) }
    }

    override fun update(update: SQLCommand.Update): AsyncResult<SQLResult.Update> {
        val sqlFuture = Future.future<UpdateResult>()
        val statement = update.statement
        when (statement) {
            is SQLStatement.Parameterized -> connection.updateWithParams(statement.query, JsonArray(statement.params), sqlFuture.completer())
            is SQLStatement.Simple -> connection.update(statement.query, sqlFuture.completer())
        }
        return VertxResult(sqlFuture)
                .map { SQLResult.Update(it.updated) }
                .recover { CompletableResult.failed(SQLError.OnCommand(update, it)) }
    }

    override fun exec(exec: SQLCommand.Exec): AsyncResult<SQLResult.Void> {
        val statement = exec.statement
        val query = when(statement) {
            is SQLStatement.Parameterized -> statement.query
            is SQLStatement.Simple -> statement.query
        }

        val sqlFuture = Future.future<Void>()
        connection.execute(query, sqlFuture.completer())
        return VertxResult(sqlFuture)
                .map { SQLResult.Void }
                .recover { CompletableResult.failed(SQLError.OnCommand(exec, it)) }
    }

    override fun <T> close(): (T) -> AsyncResult<T> = { thing ->
        val future = Future.future<Void>()
        connection.close(future.completer())
        VertxResult(future.map { thing })
    }

    override fun <T> closeOnFailure(): (Throwable) -> AsyncResult<T> = { error ->
        val future = Future.future<Void>()
        connection.close(future.completer())
        VertxResult(future.compose<T> { Future.failedFuture(error) })
    }

    override fun <T> commit(): (T) -> AsyncResult<T> = { thing ->
        val future = Future.future<Void>()
        connection.commit(future.completer())
        VertxResult(future.map { thing })
    }

    override fun <T> rollback(): (Throwable) -> AsyncResult<T> = { error ->
        val future = Future.future<Void>()
        connection.rollback(future.completer())
        VertxResult(future.compose<T> { Future.failedFuture(error) })
    }

    override fun setAutoCommit(autoCommit: Boolean): AsyncResult<Unit> {
        val confFuture: Future<Void> = Future.future()
        connection.setAutoCommit(autoCommit, confFuture.completer())
        return VertxResult(confFuture.map { Unit })
    }

    private class VertxSQLRow(val row: JsonObject): SQLRow {
        override fun getString(key: String): String {
            return row.getString(key)
        }

        override fun getBoolean(key: String): Boolean {
            return row.getBoolean(key)
        }

        override fun getLong(key: String): Long {
            return row.getLong(key)
        }

        override fun getInt(key: String): Int {
            return row.getInteger(key)
        }

        override fun getInstant(key: String): Instant {
            return row.getInstant(key)
        }
    }

    private class VertxRowIterator(val rs: Iterator<JsonObject>): Iterator<VertxSQLRow> {
        override fun hasNext(): Boolean {
            return rs.hasNext()
        }

        override fun next(): VertxSQLRow {
            return VertxSQLRow(rs.next())
        }
    }
}