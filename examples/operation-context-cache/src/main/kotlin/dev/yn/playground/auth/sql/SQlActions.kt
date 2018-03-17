package dev.yn.playground.auth.sql

import dev.yn.playground.auth.AuthSession
import dev.yn.playground.auth.context.UserSessionCache
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.sql.*
import dev.yn.playground.ex.query
import dev.yn.playground.Task
import dev.yn.playground.user.models.UserError
import dev.yn.playground.user.sql.UserSQL
import org.funktionale.tries.Try
import java.time.Instant

object AuthSQLActions {
    fun <T, R: UserSessionCache> validate(): Task<T, T, ApplicationContext<R>> =
            Task.updateContext(
                    Task.identity<T, ApplicationContext<R>>()
                            .mapWithContext { i, c -> c.cache.getUserSessionKey() }
                            .query(SelectSessionByKey)
                            .mapWithContext { session, c -> c.cache.setUserSession(session) })

    object SelectSessionByKey: SQLMapping<String, AuthSession, SQLCommand.Query, SQLResult.Query> {
        override fun mapResult(i: String, rs: SQLResult.Query): Try<AuthSession> =
                Try { rs.result.next() }
                        .map {
                                AuthSession.User(
                                        it.getString("user_id"),
                                        it.getInstant("expiration"))

                        }
                        .rescue { Try.Failure(UserError.AuthenticationFailed) }
                        .flatMap<AuthSession> {
                            if(it.expiration.isBefore(Instant.now())) {
                                Try.Failure(UserError.SessionExpired)
                            } else {
                                Try.Success(it)
                            } }



        override fun toSql(i: String): SQLCommand.Query =
                SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(i)))
    }
}