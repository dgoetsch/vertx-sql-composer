package dev.yn.playground.user.extensions.schema

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.user.sql.UserSchema
import devyn.playground.sql.task.SQLTransactionTask

fun ApplicationContext.initUserSchema() = SQLTransactionTask.transaction(UserSchema.init()).run(Unit, this)
fun ApplicationContext.dropUserSchema() = SQLTransactionTask.autoCommit(UserSchema.drop()).run(Unit, this)

fun <T> AsyncResult<T>.dropUserSchema(context: ApplicationContext) = this.flatMap { context.dropUserSchema() }
fun <T> AsyncResult<T>.initUserSchema(context: ApplicationContext) = this.flatMap{ context.initUserSchema() }