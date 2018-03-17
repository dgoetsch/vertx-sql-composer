package dev.yn.playground.sql

import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import kotlinx.coroutines.experimental.launch
import org.funktionale.tries.Try
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class CoroutineJDBCExecutor(val connection: Connection): SQLExecutor() {
    override fun query(query: SQLCommand.Query): AsyncResult<SQLResult.Query> {
        val statement = query.statement
        return runAsync { Try { when (statement) {
            is SQLStatement.Simple ->
                connection.createStatement().executeQuery(statement.query)
            is SQLStatement.Parameterized -> {
                val preparedStatement = connection.prepareStatement(statement.query)
                statement.params.forEachIndexed { idx, value -> preparedStatement.setObject(idx + 1, value) }
                preparedStatement.executeQuery()
            } } }
                .map { SQLResult.Query(JDBCIterator(it)) } }
    }

    override fun update(update: SQLCommand.Update): AsyncResult<SQLResult.Update> {
        val statement = update.statement
        return runAsync { Try { when(statement) {
            is SQLStatement.Simple -> connection.createStatement().executeUpdate(statement.query)
            is SQLStatement.Parameterized -> {
                val preparedStatement = connection.prepareStatement(statement.query)
                statement.params.forEachIndexed { idx, value -> preparedStatement.setObject(idx + 1, value) }
                preparedStatement.executeUpdate()
            } } }
                .map { SQLResult.Update(it) } }
    }

    override fun exec(exec: SQLCommand.Exec): AsyncResult<SQLResult.Void> {
        val statement = exec.statement
        return runAsync { Try { when(statement) {
            is SQLStatement.Simple ->
                connection.createStatement().execute(statement.query)
            is SQLStatement.Parameterized -> {
                val preparedStatement = connection.prepareStatement(statement.query)
                statement.params.forEachIndexed { idx, value -> preparedStatement.setObject(idx + 1, value) }
                preparedStatement.execute()
            } } } }
                .map { SQLResult.Void }
    }

    override fun <T> close(): (T) -> AsyncResult<T> = { t ->
        runAsync {
            Try { connection.close() }
                    .map { t }
        }
    }

    override fun <T> closeOnFailure(): (Throwable) -> AsyncResult<T> = { f ->
        runAsync {
            Try { connection.close() }
                    .flatMap { Try.Failure<T>(f) }
        }
    }

    override fun <T> commit(): (T) -> AsyncResult<T> = { t ->
        runAsync {
            Try { connection.commit() }
                    .map { t }
        }
    }

    override fun <T> rollback(): (Throwable) -> AsyncResult<T> = { f ->
        runAsync {
            Try { connection.rollback() }
                    .flatMap { Try.Failure<T>(f) }
        }
    }

    override fun setAutoCommit(autoCommit: Boolean): AsyncResult<Unit> {
        return runAsync {
            Try { connection.setAutoCommit(autoCommit) }
        }
    }

    fun <T> runAsync(action: () -> Try<T>): AsyncResult<T> {
        val asyncResult = CompletableResult<T>()
        launch {
            action()
                    .onSuccess(asyncResult::succeed)
                    .onFailure(asyncResult::fail)
        }
        return asyncResult
    }

    private class JDBCSQLRow(val row: Map<String, Any>): SQLRow {
        companion object {
            fun of(row: ResultSet): JDBCSQLRow {
                val row = (1..row.metaData.columnCount)
                        .map {
                            row.metaData.getColumnName(it) to when(row.metaData.getColumnType(it)) {
                                Types.BOOLEAN -> row.getBoolean(it)
                                Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.NCHAR, Types.CHAR -> row.getString(it)
                                Types.INTEGER -> row.getInt(it)
                                Types.BIGINT -> row.getLong(it)
                                Types.TIME -> row.getTime(it)
                                Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> row.getTimestamp(it)
                                Types.DATE -> row.getDate(it)
                                else -> row.getObject(it)
                            } }
                        .toMap()
                return JDBCSQLRow(row)
            }
        }

        override fun getString(key: String): String {
            val result = row.get(key)
            when(result) {
                is String -> return result
                else -> throw SQLError.SQLRowError(result, "Value was not a string")
            }
        }

        override fun getBoolean(key: String): Boolean {
            val result = row.get(key)
            when(result) {
                is Boolean -> return result
                else -> throw SQLError.SQLRowError(result, "Value was not a boolean")
            }
        }

        override fun getLong(key: String): Long {
            val result = row.get(key)
            return when(result) {
                is Long -> result
                else -> throw SQLError.SQLRowError(result, "Value was not a long")
            }
        }

        override fun getInt(key: String): Int {
            val result = row.get(key)
            when(result) {
                is Int -> return result
                else -> throw SQLError.SQLRowError(result, "Value was not an int")
            }
        }

        override fun getInstant(key: String): Instant {
            val result = row.get(key)
            when(result) {
                is Timestamp -> return result.toInstant()
                else -> throw SQLError.SQLRowError(result, "Value was not an instant")
            }
        }
    }

    private class JDBCIterator(val rs: ResultSet): Iterator<JDBCSQLRow> {
        val queue = LinkedBlockingQueue<JDBCSQLRow>()
        override fun hasNext(): Boolean {
            if(queue.isNotEmpty()) {
                return true
            } else if(rs.next()) {
                queue.offer(JDBCSQLRow.of(rs))
                return true
            } else {
                return false
            }
        }

        override fun next(): JDBCSQLRow {
            return Optional.of(hasNext())
                    .filter { it }
                    .flatMap { Optional.of(queue.poll()) }
                    .orElseThrow { SQLError.RowIteratorEmpty }
        }

    }
}