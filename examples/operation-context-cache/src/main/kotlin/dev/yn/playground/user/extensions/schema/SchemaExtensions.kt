package dev.yn.playground.user.extensions.schema

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.user.sql.UserSchema
import devyn.playground.sql.task.SQLTransactionTask

fun ApplicationContext<Unit>.initUserSchema() = SQLTransactionTask.transaction<Unit, Unit, ApplicationContext<Unit>>(UserSchema.init()).run(Unit, this)
fun ApplicationContext<Unit>.dropUserSchema() = SQLTransactionTask.autoCommit<Unit, Unit, ApplicationContext<Unit>>(UserSchema.drop()).run(Unit, this)

fun <T> AsyncResult<T>.dropUserSchema(context: ApplicationContext<Unit>) = this.flatMap { context.dropUserSchema() }
fun <T> AsyncResult<T>.initUserSchema(context: ApplicationContext<Unit>) = this.flatMap{ context.initUserSchema() }