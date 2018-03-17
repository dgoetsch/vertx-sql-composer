package dev.yn.playground.user.extensions.transaction

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.user.sql.UserTransactions
import devyn.playground.sql.task.SQLTransactionTask

fun ApplicationContext<Unit>.deleteAllUsers() = SQLTransactionTask.autoCommit<Unit, Unit, ApplicationContext<Unit>>(UserTransactions.deleteAllUserActionChain()).run(Unit, this)