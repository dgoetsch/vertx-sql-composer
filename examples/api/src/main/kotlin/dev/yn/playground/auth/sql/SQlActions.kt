package dev.yn.playground.auth.sql

import dev.yn.playground.auth.AuthSession
import dev.yn.playground.auth.SessionAndInput
import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.sql.*
import dev.yn.playground.Task
import dev.yn.playground.user.models.UserError
import dev.yn.playground.user.sql.UserSQL
import org.funktionale.tries.Try
import java.time.Instant

object AuthSQLActions {
    fun <T> validateAction() =
            Task.identity<TokenAndInput<T>, ApplicationContext>()
                    .query(SelectSessionByKey())

    class SelectSessionByKey<T>: SQLMapping<TokenAndInput<T>, SessionAndInput<T>, SQLCommand.Query, SQLResult.Query> {
        override fun mapResult(i: TokenAndInput<T>, rs: SQLResult.Query): Try<SessionAndInput<T>> =
                Try { rs.result.next() }
                        .map {SessionAndInput(
                                AuthSession.User(
                                        it.getString("user_id"),
                                        it.getInstant("expiration")),
                                i.input)

                        }
                        .rescue { Try.Failure(UserError.AuthenticationFailed) }
                        .flatMap {
                            if(it.session.expiration.isBefore(Instant.now())) {
                                Try.Failure<SessionAndInput<T>>(UserError.SessionExpired)
                            } else {
                                Try.Success(it)
                            } }



        override fun toSql(i: TokenAndInput<T>): SQLCommand.Query =
                SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(i.token)))
    }
}