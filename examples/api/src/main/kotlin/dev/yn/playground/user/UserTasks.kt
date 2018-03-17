package dev.yn.playground.user

import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.Task
import dev.yn.playground.user.models.UserNameAndPassword
import dev.yn.playground.user.models.UserProfile
import dev.yn.playground.user.models.UserProfileAndPassword
import dev.yn.playground.user.models.UserSession
import dev.yn.playground.user.sql.UserTransactions
import devyn.playground.sql.task.SQLTransactionTask

object UserTasks {
    val unpreparedCreateTask: Task<UserProfileAndPassword, UserProfile, ApplicationContext> =
            SQLTransactionTask.transaction(UserTransactions.createUserActionChain)

    val unpreparedLoginTask: Task<UserNameAndPassword, UserSession, ApplicationContext> =
            SQLTransactionTask.transaction(UserTransactions.loginActionChain)


    val unpreparedGetTask: Task<TokenAndInput<String>, UserProfile, ApplicationContext> =
            SQLTransactionTask.autoCommit(UserTransactions.getUserActionChain)
}