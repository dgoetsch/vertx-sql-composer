package dev.yn.playground.user.sql

import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.sql.*
import dev.yn.playground.user.models.*
import org.funktionale.tries.Try
import java.sql.Timestamp
import java.time.Instant

object UserSQL {
    val selectSessionByKey= "SELECT session_key, user_id, expiration FROM user_session where session_key = ?"
}

object InsertUserProfileMapping: SQLUpdateMapping<UserProfileAndPassword, UserProfileAndPassword> {
    val insertUser = "INSERT INTO user_profile (id, user_name, allow_public_message) VALUES (?, ?, ?)"

    override fun toSql(i: UserProfileAndPassword): SQLCommand.Update =
            SQLCommand.Update(SQLStatement.Parameterized(insertUser, listOf(i.userProfile.id, i.userProfile.name, i.userProfile.allowPublicMessage)))

    override fun mapResult(i: UserProfileAndPassword, rs: SQLResult.Update): Try<UserProfileAndPassword> =
            if(rs.count == 1) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
}

object InsertUserPasswordMapping: SQLUpdateMapping<UserProfileAndPassword, UserProfile> {
    val insertUserPassword = "INSERT into user_password (user_id, pswhash) VALUES (?, crypt(?, gen_salt('bf')))"

    override fun toSql(i: UserProfileAndPassword): SQLCommand.Update =
            SQLCommand.Update(SQLStatement.Parameterized(insertUserPassword, listOf(i.userProfile.id, i.password)))

    override fun mapResult(i: UserProfileAndPassword, rs: SQLResult.Update): Try<UserProfile> =
            if(rs.count == 1) Try.Success(i.userProfile) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
}

object ValidatePasswordForUserId : SQLQueryMapping<UserIdAndPassword, String> {
    val selectUserPassword = "SELECT user_id FROM user_password WHERE user_id = ? AND pswhash = crypt(?, pswhash)"

    override fun toSql(i: UserIdAndPassword): SQLCommand.Query =
            SQLCommand.Query(SQLStatement.Parameterized(selectUserPassword, listOf(i.id, i.password)))

    override fun mapResult(i: UserIdAndPassword, rs: SQLResult.Query): Try<String> =
            Try { rs.result.iterator().next() }
                    .map { it.getString("user_id") }
                    .rescue { Try.Failure(UserError.AuthenticationFailed) }
}

object SelectUserIdForLogin : SQLQueryMapping<UserNameAndPassword, UserIdAndPassword> {
    val selectUserId = "SELECT id from user_profile WHERE user_name = ?"

    override fun toSql(i: UserNameAndPassword): SQLCommand.Query = SQLCommand.Query(SQLStatement.Parameterized(selectUserId, listOf(i.userName)))


    override fun mapResult(i: UserNameAndPassword, rs: SQLResult.Query): Try<UserIdAndPassword> =
            Try { rs.result.iterator().next() }
                    .map { UserIdAndPassword(it.getString("id"), i.password) }
                    .rescue { Try.Failure(UserError.NoSuchUser(i.userName)) }
}

object InsertSession: SQLUpdateMapping<UserSession, UserSession> {
    val insertSession = "INSERT INTO user_session (session_key, user_id, expiration) VALUES (?, ?, ?)"

    override fun toSql(i: UserSession): SQLCommand.Update =
            SQLCommand.Update(SQLStatement.Parameterized(insertSession, listOf(i.sessionKey, i.userId, Timestamp.from(i.expiration))))

    override fun mapResult(i: UserSession, rs: SQLResult.Update): Try<UserSession> =
            if(rs.count == 1) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
}

class SelectSessionByKey<T>(val validateSesssion: (UserSession, T) -> Try<T>): SQLQueryMapping<TokenAndInput<T>, T> {
    override fun mapResult(i: TokenAndInput<T>, rs: SQLResult.Query): Try<T> =
        Try { rs.result.iterator().next() }
                .map {  UserSession(
                        it.getString("session_key"),
                        it.getString("user_id"),
                        it.getInstant("expiration"))
                }
                .flatMap {
                    if(it.expiration.isBefore(Instant.now())) {
                        Try.Failure(UserError.SessionExpired)
                    } else {
                        validateSesssion(it, i.input)
                    } }
                .rescue { Try.Failure(UserError.AuthenticationFailed) }


    override fun toSql(i: TokenAndInput<T>): SQLCommand.Query =
            SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(i.token)))
}

object SelectUserProfileById: SQLQueryMapping<String, UserProfile> {
    val selectUser = "SELECT id, user_name, allow_public_message FROM user_profile where id = ?"
    override fun toSql(i: String): SQLCommand.Query = SQLCommand.Query(SQLStatement.Parameterized(selectUser, listOf(i)))

    override fun mapResult(i: String, rs: SQLResult.Query): Try<UserProfile> =
            Try { rs.result.next() }
                    .rescue { Try.Failure(UserError.NoSuchUser(i)) }
                    .map {
                        UserProfile(it.getString("id"),
                                it.getString("user_name"),
                                it.getBoolean("allow_public_message")) }

}

class SelectUserSessionExists<T>(val onResult:(String, Boolean) -> Try<T>): SQLQueryMapping<String, T> {
    val selectUserSessionExists = "SELECT exists(select user_id from user_session where user_id = ?) as succeeded"

    override fun toSql(i: String): SQLCommand.Query =
            SQLCommand.Query(SQLStatement.Parameterized(selectUserSessionExists, listOf(i)))

    override fun mapResult(i: String, rs: SQLResult.Query): Try<T> =
            Try { rs.result.next() }
                    .map { it.getBoolean("succeeded") }
                    .flatMap { onResult(i, it) }
}

val EnsureNoSessionExists = SelectUserSessionExists({ userId, exists ->
    if (exists) {
        Try.Failure<String>(UserError.SessionAlreadyExists(userId))
    } else {
        Try.Success(userId)
    }
})

object InsertTrustedDevice: SQLUpdateMapping<UserTrustedDevice, UserTrustedDevice> {
    val insertTrustedDevice = "INSERT INTO user_trusted_device (device_key, user_id, device_name, expiration) VALUES (?, ?, ?, ?)"

    override fun toSql(i: UserTrustedDevice): SQLCommand.Update =
            SQLCommand.Update(SQLStatement.Parameterized(insertTrustedDevice, listOf(i.deviceKey, i.userId, i.deviceName, Timestamp.from(i.expiration))))

    override fun mapResult(i: UserTrustedDevice, rs: SQLResult.Update): Try<UserTrustedDevice> =
            if(rs.count== 1) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
}

object SelectUserSessionFromTrustedDevice: SQLQueryMapping<UserTrustedDevice, UserSession> {
    val selectUserSessionFromTrustedDevice =
            "SELECT user_session.session_key, user_session.user_id, user_session.expiration from user_trusted_device " +
                    "LEFT JOIN user_session on user_trusted_device.user_id = user_session.user_id " +
                    "WHERE user_trusted_device.device_key=? AND user_trusted_device.user_id=?"

    override fun toSql(i: UserTrustedDevice): SQLCommand.Query =
            SQLCommand.Query(SQLStatement.Parameterized(selectUserSessionFromTrustedDevice, listOf(i.deviceKey, i.userId)))

    override fun mapResult(i: UserTrustedDevice, rs: SQLResult.Query): Try<UserSession> =
        Try { rs.result.next() }
                .rescue { Try.Failure(UserError.NoSuchTrustedDevice(i)) }
                .map {
                    UserSession(
                            it.getString("session_key"),
                            it.getString("user_id"),
                            it.getInstant("expiration"))
                }
}