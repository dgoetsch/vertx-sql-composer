package dev.yn.playground.auth.context

import dev.yn.playground.auth.AuthSession
import org.funktionale.option.Option

interface UserSessionCache {
    companion object {
        fun create(sessionKey: String): UserSessionCache = NothingElse(sessionKey)
    }

    fun getUserSessionKey(): String
    fun setUserSession(userSession: AuthSession)
    fun getUserSession(): Option<AuthSession>

    private data class NothingElse(val sessionKey: String, var session: Option<AuthSession> = Option.None): UserSessionCache {
        override fun getUserSessionKey(): String = sessionKey

        override fun setUserSession(userSession: AuthSession) {
            this.session = Option.Some(userSession)
        }

        override fun getUserSession(): Option<AuthSession> = session
    }
}