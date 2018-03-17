package dev.yn.playground.auth

import java.time.Instant

/**
 * Created by devyn on 10/1/17.
 */
sealed class AuthSession {
    abstract val userId: String
    abstract val isAnonymous: Boolean
    abstract val expiration: Instant
    data class User(override val userId: String, override val expiration: Instant): AuthSession() {
        override val isAnonymous: Boolean = false
    }

}

data class TokenAndInput<T>(val token: String, val input: T)
data class SessionAndInput<T>(val session: AuthSession, val input: T)