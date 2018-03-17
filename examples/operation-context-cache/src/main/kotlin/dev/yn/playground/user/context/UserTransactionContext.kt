package dev.yn.playground.user.context

import dev.yn.playground.auth.AuthSession
import dev.yn.playground.auth.context.UserSessionCache
import dev.yn.playground.user.models.UserProfile
import org.funktionale.option.Option

interface ExistingUserProfileContext {
    fun getExistingProfile(): Option<UserProfile>
    fun useProfile(profile: UserProfile)
}

class GetUserContext(val sessionKey: String, var session: Option<AuthSession> = Option.None): UserSessionCache {
    override fun getUserSessionKey(): String = sessionKey

    override fun setUserSession(userSession: AuthSession) {
        session = Option.Some(userSession)
    }

    override fun getUserSession(): Option<AuthSession> = session
}