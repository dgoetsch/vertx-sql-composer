package dev.yn.playground.sql

import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.ex.query
import dev.yn.playground.ex.update
import dev.yn.playground.Task
import dev.yn.playground.andThen
import devyn.playground.sql.task.SQLTransactionTask
import io.kotlintest.mock.mock
import io.kotlintest.specs.StringSpec
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.time.Instant

class SQLTaskSpec: StringSpec() {
    data class UserSession(val sessionKey: String, val userId: String, val expiration: Instant)



    data class ApplicationContext<R>(val sqlExecutor: SQLExecutor, val cache: R):
            SQLTaskContext<SQLExecutor>,
            OperationCache<R> {
        override fun getOperationCache(): R = cache

        override fun getSQLExecutor(): SQLExecutor {
            return sqlExecutor
        }
    }

    interface OperationCache<R> {
        fun getOperationCache(): R
    }

    interface UserSessionContext {
        fun getUserSessionKey(): String
        fun setUserSession(userSession: UserSession)
        fun getUserSession(): Option<UserSession>
    }

    interface ExistingUserProfileContext {
        fun getExistingProfile(): Option<UserProfile>
        fun useProfile(profile: UserProfile)
    }

    data class UpdateUserProfileContext(val sessionKey: String,
                                        var session: Option<UserSession> = Option.None,
                                        var existing: Option<UserProfile> = Option.None): UserSessionContext, ExistingUserProfileContext {
        override fun getUserSessionKey(): String = this.sessionKey

        override fun setUserSession(userSession: UserSession) {
            this.session = Option.Some(userSession)
        }

        override fun getUserSession(): Option<UserSession> = this.session

        override fun getExistingProfile(): Option<UserProfile> = this.existing

        override fun useProfile(profile: UserProfile) {
            this.existing = Option.Some(profile)
        }
    }


    data class UserProfile(val id: String, val name: String, val allowPublicMessage: Boolean)

    object SelectUserProfileById: SQLQueryMapping<String, UserProfile> {
        val selectUser = "SELECT id, user_name, allow_public_message FROM user_profile where id = ?"
        override fun toSql(i: String): SQLCommand.Query = SQLCommand.Query(SQLStatement.Parameterized(selectUser, listOf(i)))

        override fun mapResult(i: String, rs: SQLResult.Query): Try<UserProfile> =
                Try { rs.result.next() }
                        .rescue { Try.Failure(IllegalArgumentException("no such user")) }
                        .map {
                            UserProfile(it.getString("id"),
                                    it.getString("user_name"),
                                    it.getBoolean("allow_public_message")) }

    }

    object UpdateUserProfile: SQLUpdateMapping<UserProfile, UserProfile> {
        override fun toSql(i: UserProfile): SQLCommand.Update {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun mapResult(i: UserProfile, rs: SQLResult.Update): Try<UserProfile> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object SelectSessionByKey: SQLQueryMapping<String, UserSession> {
        override fun toSql(i: String): SQLCommand.Query {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun mapResult(i: String, rs: SQLResult.Query): Try<UserSession> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    val onlyIfSessionUserIdMathcesRequested = { update: UserProfile, context: ApplicationContext<UpdateUserProfileContext> ->
        context.getOperationCache().getUserSession()
                .map { it.userId }
                .filter { update.id == it }
                .map { Try.Success(update) }
                .getOrElse { Try.Failure<UserProfile>(IllegalArgumentException("not authorized")) }
    }

    val authenticateUpdateUserProfile = Task.mapTryWithContext(onlyIfSessionUserIdMathcesRequested)

    fun <R: ExistingUserProfileContext> addExistingUserToContext() =
            Task.updateContext(Task.identity<UserProfile, ApplicationContext<R>>()
                    .map { it.id }
                    .query(SelectUserProfileById)
                    .mapWithContext { existing, context ->
                        context.getOperationCache().useProfile(existing)
                    })

    fun <R: ExistingUserProfileContext> failIfProfileNotCached() = Task.mapTryWithContext<UserProfile, UserProfile, ApplicationContext<R>> {
        i, context ->
        context.getOperationCache().getExistingProfile()
                .map { Try.Success(i) }
                .getOrElse { Try.Failure<UserProfile>(IllegalArgumentException("No such user")) }
    }

    fun <I, R: UserSessionContext> validateSession(): Task<I, I, ApplicationContext<R>> =
            Task.updateContext(Task.identity<I, ApplicationContext<R>>()
                            .mapWithContext { _, c -> c.cache.getUserSessionKey()}
                            .query(SelectSessionByKey)
                            .mapWithContext { session, c -> c.cache.setUserSession(session) })


    init {
        "Do a thing" {
            val executor = mock<SQLExecutor>()
            val sessionKey = "TEST_SESSION_KEY"
            val context = ApplicationContext<UpdateUserProfileContext>(executor, UpdateUserProfileContext(sessionKey))

            val udpateUser: SQLTransactionTask<UserProfile, UserProfile, ApplicationContext<UpdateUserProfileContext>> =
                    SQLTransactionTask.transaction(
                                validateSession<UserProfile, UpdateUserProfileContext>()
                                    .andThen(authenticateUpdateUserProfile)
                                    .andThen(addExistingUserToContext<UpdateUserProfileContext>())
                                    .andThen(failIfProfileNotCached<UpdateUserProfileContext>())
                                    .update(UpdateUserProfile))


        }
    }
}