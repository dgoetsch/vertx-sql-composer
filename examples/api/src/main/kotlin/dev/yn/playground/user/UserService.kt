package dev.yn.playground.user

import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.user.models.UserNameAndPassword
import dev.yn.playground.user.models.UserProfile
import dev.yn.playground.user.models.UserProfileAndPassword
import dev.yn.playground.user.models.UserSession

class UserService(val provider: ApplicationContextProvider) {
    fun createUser(userProfile: UserProfileAndPassword): AsyncResult<UserProfile> =
            provider.runOnContext(UserTasks.unpreparedCreateTask, userProfile)

    fun loginUser(userNameAndPassword: UserNameAndPassword): AsyncResult<UserSession> =
            provider.runOnContext(UserTasks.unpreparedLoginTask, userNameAndPassword)

    fun getUser(userId: String, token: String): AsyncResult<UserProfile> =
            provider.runOnContext(UserTasks.unpreparedGetTask, TokenAndInput(token, userId))
}
