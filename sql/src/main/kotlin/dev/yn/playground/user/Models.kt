package dev.yn.playground.user

import java.time.Instant

data class UserProfile(val id: String, val name: String, val allowPubliMessage: Boolean)
data class UserNameAndPassword(val userName: String, val password: String)
data class UserIdAndPassword(val id: String, val password: String)
data class UserProfileAndPassword(val userProfile: UserProfile, val password: String)
data class UserSession(val sessionKey: String, val userId: String, val expiration: Instant)