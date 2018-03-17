package dev.yn.playground.user

import dev.yn.playground.user.models.UserProfile
import dev.yn.playground.user.models.UserProfileAndPassword
import java.util.*

object UserFixture {
    fun generateUser(num: Int): UserProfileAndPassword {
        val userId = UUID.randomUUID().toString()
        val password = "pass$num"
        val userName = "user$num"
        val user = UserProfile(userId, userName, false)
        return UserProfileAndPassword(user, password)
    }
}