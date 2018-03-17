package dev.yn.playground.user.sql

import dev.yn.playground.auth.sql.AuthSQLActions
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.publisher.PublishCommand
import dev.yn.playground.ex.deleteAll
import dev.yn.playground.ex.query
import dev.yn.playground.ex.update
import dev.yn.playground.Task
import dev.yn.playground.user.models.*
import org.funktionale.tries.Try
import java.time.Instant
import java.util.*
import dev.yn.playground.ex.*
import dev.yn.playground.ex.deserialize
import dev.yn.playground.ex.serialize
import dev.yn.playground.andThen
import dev.yn.playground.user.context.GetUserContext
import dev.yn.playground.user.userCreatedAddress
import dev.yn.playground.user.userLoginAddress
import org.funktionale.option.getOrElse

object UserTransactions {
    private val createNewSessionKey: (String) -> UserSession = { UserSession(UUID.randomUUID().toString(), it, Instant.now().plusSeconds(3600)) }

    private val publishUserCreateEvent: Task<UserProfile, UserProfile, ApplicationContext<Unit>> =
            Task.identity<UserProfile, ApplicationContext<Unit>>()
                    .serialize()
                    .publish { PublishCommand.Publish(userCreatedAddress, it) }
                    .deserialize(UserProfile::class.java)

    private val publishUserLoginEvent: Task<UserSession, UserSession, ApplicationContext<Unit>> =
            Task.identity<UserSession, ApplicationContext<Unit>>()
                    .serialize()
                    .publish { PublishCommand.Publish(userLoginAddress, it) }
                    .deserialize(UserSession::class.java)

    private val authorizeUser: Task<String, String, ApplicationContext<GetUserContext>> = Task.mapTryWithContext { userId, context ->
        context.cache.getUserSession()
                .filter { it.userId == userId }
                .map { Try.Success(userId) }
                .getOrElse { Try.Failure<String>(UserError.AuthorizationFailed) }
    }

    val createUserActionChain: Task<UserProfileAndPassword, UserProfile, ApplicationContext<Unit>> =
            Task.identity<UserProfileAndPassword, ApplicationContext<Unit>>()
                    .update(InsertUserProfileMapping)
                    .update(InsertUserPasswordMapping)
                    .andThen(publishUserCreateEvent)

    val loginActionChain: Task<UserNameAndPassword, UserSession, ApplicationContext<Unit>> =
            Task.identity<UserNameAndPassword, ApplicationContext<Unit>>()
                    .query(SelectUserIdForLogin)
                    .query(ValidatePasswordForUserId)
                    .query(EnsureNoSessionExists)
                    .map(createNewSessionKey)
                    .update(InsertSession)
                    .andThen(publishUserLoginEvent)

    private val onlyIfRequestedUserMatchesSessionUser =  { session: UserSession, userId: String ->
        if (session.userId == userId) {
            Try.Success(userId)
        } else {
            Try.Failure<String>(UserError.AuthorizationFailed)
        }
    }

    val getUserActionChain: Task<String, UserProfile, ApplicationContext<GetUserContext>> =
            AuthSQLActions.validate<String, GetUserContext>()
                    .andThen(authorizeUser)
                    .query(SelectUserProfileById)

    fun deleteAllUserActionChain() = Task.identity<Unit, ApplicationContext<Unit>>()
            .deleteAll("user_relationship_request")
            .deleteAll("user_password")
            .deleteAll("user_session")
            .deleteAll("user_profile")


}