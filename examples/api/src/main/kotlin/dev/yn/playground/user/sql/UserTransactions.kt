package dev.yn.playground.user.sql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.yn.playground.auth.TokenAndInput
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
import dev.yn.playground.user.userCreatedAddress
import dev.yn.playground.user.userLoginAddress

object UserTransactions {
    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

    private val createNewSessionKey: (String) -> UserSession = { UserSession(UUID.randomUUID().toString(), it, Instant.now().plusSeconds(3600)) }
    private val publishUserCreateEvent: (UserProfile) -> PublishCommand.Publish =
            { PublishCommand.Publish(userCreatedAddress, objectMapper.writeValueAsBytes(it))}
    private val publishUserLoginEvent: (UserSession) -> PublishCommand.Publish =
            { PublishCommand.Publish(userLoginAddress, objectMapper.writeValueAsBytes(it))}


    val createUserActionChain: Task<UserProfileAndPassword, UserProfile, ApplicationContext> =
            Task.identity<UserProfileAndPassword, ApplicationContext>()
                    .update(InsertUserProfileMapping)
                    .update(InsertUserPasswordMapping)
                    .publish(publishUserCreateEvent)

    val loginActionChain: Task<UserNameAndPassword, UserSession, ApplicationContext> =
            Task.identity<UserNameAndPassword, ApplicationContext>()
                    .query(SelectUserIdForLogin)
                    .query(ValidatePasswordForUserId)
                    .query(EnsureNoSessionExists)
                    .map(createNewSessionKey)
                    .update(InsertSession)
                    .publish(publishUserLoginEvent)

    private val onlyIfRequestedUserMatchesSessionUser =  { session: UserSession, userId: String ->
        if (session.userId == userId) {
            Try.Success(userId)
        } else {
            Try.Failure<String>(UserError.AuthorizationFailed)
        }
    }

    val getUserActionChain: Task<TokenAndInput<String>, UserProfile, ApplicationContext> =
            validateSession<String>(onlyIfRequestedUserMatchesSessionUser)
                    .query(SelectUserProfileById)

    private fun <T> validateSession(validateSession: (UserSession, T) -> Try<T>): Task<TokenAndInput<T>, T, ApplicationContext> =
            Task.identity<TokenAndInput<T>, ApplicationContext>()
                    .query(SelectSessionByKey(validateSession))

    fun deleteAllUserActionChain(): Task<Unit, Unit, ApplicationContext> =
            Task.identity<Unit, ApplicationContext>()
                    .deleteAll("user_relationship_request")
                    .deleteAll("user_password")
                    .deleteAll("user_session")
                    .deleteAll("user_profile")


}