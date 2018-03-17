package dev.yn.playground.user

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.Task
import dev.yn.playground.user.context.GetUserContext
import dev.yn.playground.user.models.UserNameAndPassword
import dev.yn.playground.user.models.UserProfile
import dev.yn.playground.user.models.UserProfileAndPassword
import dev.yn.playground.user.models.UserSession
import dev.yn.playground.user.sql.UserTransactions
import devyn.playground.sql.task.SQLTransactionTask

object UserTasks {
    val createUserTask: Task<UserProfileAndPassword, UserProfile, ApplicationContext<Unit>> =
            SQLTransactionTask.transaction(UserTransactions.createUserActionChain)

    val loginUserTask: Task<UserNameAndPassword, UserSession, ApplicationContext<Unit>> =
            SQLTransactionTask.transaction(UserTransactions.loginActionChain)


    val getUserTask: Task<String, UserProfile, ApplicationContext<GetUserContext>> =
            SQLTransactionTask.autoCommit(UserTransactions.getUserActionChain)
}