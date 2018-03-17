package dev.yn.playground.sql

sealed class SQLError: Throwable() {
    data class UpdateMappingFailed(val mapping: SQLMapping<*, *, *, *>, val i: Any): SQLError()
    data class SQLExecutionFailed(val exec: SQLCommand.Exec): SQLError()
    data class SQLRowError(val rowValue: Any?, override val message: String): SQLError()
    object RowIteratorEmpty: SQLError()
    data class OnCommand(val statement: SQLCommand, val underlying: Throwable): SQLError()
}