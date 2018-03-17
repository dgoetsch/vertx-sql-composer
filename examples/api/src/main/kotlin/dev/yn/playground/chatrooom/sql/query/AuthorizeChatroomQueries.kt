package dev.yn.playground.chatrooom.sql.query

import dev.yn.playground.auth.SessionAndInput
import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.auth.sql.AuthSQLActions
import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomPermissionKey
import dev.yn.playground.chatrooom.models.ChatRoomPermissions
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.sql.SQLCommand
import dev.yn.playground.sql.SQLQueryMapping
import dev.yn.playground.sql.SQLResult
import dev.yn.playground.sql.SQLStatement
import dev.yn.playground.Task
import dev.yn.playground.user.models.UserError
import org.funktionale.tries.Try

fun <T> authenticate(): Task<TokenAndInput<T>, SessionAndInput<T>, ApplicationContext> {
    return AuthSQLActions.validateAction()
}

fun authorizeChatroomSelectStatement(chatRoomId: String, userId: String, permissionKey: String): SQLStatement.Parameterized {
    return SQLStatement.Parameterized(
            """SELECT NOT exists (
                |SELECT user_id FROM chatroom_user_banned WHERE chatroom_id=? AND user_id=?
            |) AND exists(
                |SELECT user_id
                |FROM chatroom_user_permission
                |WHERE chatroom_id=? AND user_id=? AND permission_key=?
                |UNION
                |SELECT 'anon'
                |FROM chatroom_permission
                |WHERE chatroom_id=? AND permission_key=? AND allow_public=true) AS authorized""".trimMargin(),
            listOf(chatRoomId, userId, chatRoomId, userId, permissionKey, chatRoomId, permissionKey))
}

fun <T> mapAuthResultSet(input: T, rs: SQLResult.Query): Try<T> {
    return Try { rs.result.next() }
            .map { it.getBoolean("authorized") }
            .filter { it }
            .map { input }
            .rescue { Try.Failure<T>(UserError.AuthorizationFailed) }
}


object AuthorizeChatRoomAddUser: SQLQueryMapping<SessionAndInput<ChatRoomUser>, SessionAndInput<ChatRoomUser>> {
    override fun toSql(i: SessionAndInput<ChatRoomUser>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.AddUser.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomUser>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomUser>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizeChatRoomRemoveUser: SQLQueryMapping<SessionAndInput<ChatRoomUser>, SessionAndInput<ChatRoomUser>> {
    override fun toSql(i: SessionAndInput<ChatRoomUser>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.RemoveUser.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomUser>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomUser>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorzeChatRoomUpdate: SQLQueryMapping<SessionAndInput<ChatRoom>, SessionAndInput<ChatRoom>> {
    override fun toSql(i: SessionAndInput<ChatRoom>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.id, i.session.userId, ChatRoomPermissionKey.Update.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoom>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoom>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizedGetChatroom : SQLQueryMapping<SessionAndInput<String>, SessionAndInput<String>> {
    override fun toSql(i: SessionAndInput<String>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input, i.session.userId, ChatRoomPermissionKey.Get.key))
    }

    override fun mapResult(i: SessionAndInput<String>, rs: SQLResult.Query): Try<SessionAndInput<String>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthrorizeCreateChatroom : SQLQueryMapping<SessionAndInput<ChatRoom>, SessionAndInput<ChatRoom>> {
    override fun toSql(i: SessionAndInput<ChatRoom>): SQLCommand.Query {
        return SQLCommand.Query(SQLStatement.Simple("SELECT true AS authorized"))
    }

    override fun mapResult(i: SessionAndInput<ChatRoom>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoom>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizeAddPublicPermission: SQLQueryMapping<SessionAndInput<ChatRoomPermissions>, SessionAndInput<ChatRoomPermissions>> {
    override fun toSql(i: SessionAndInput<ChatRoomPermissions>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.AddPublicPermission.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomPermissions>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomPermissions>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizeRemovePublicPermission: SQLQueryMapping<SessionAndInput<ChatRoomPermissions>, SessionAndInput<ChatRoomPermissions>> {
    override fun toSql(i: SessionAndInput<ChatRoomPermissions>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.RemovePublicPermission.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomPermissions>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomPermissions>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizeAddUserPermission: SQLQueryMapping<SessionAndInput<ChatRoomUser>, SessionAndInput<ChatRoomUser>> {
    override fun toSql(i: SessionAndInput<ChatRoomUser>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.AddUserPermission.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomUser>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomUser>> {
        return mapAuthResultSet(i, rs)
    }
}

object AuthorizeRemoveUserPermission: SQLQueryMapping<SessionAndInput<ChatRoomUser>, SessionAndInput<ChatRoomUser>> {
    override fun toSql(i: SessionAndInput<ChatRoomUser>): SQLCommand.Query {
        return SQLCommand.Query(authorizeChatroomSelectStatement(i.input.chatroom.id, i.session.userId, ChatRoomPermissionKey.RemoveUserPermission.key))
    }

    override fun mapResult(i: SessionAndInput<ChatRoomUser>, rs: SQLResult.Query): Try<SessionAndInput<ChatRoomUser>> {
        return mapAuthResultSet(i, rs)
    }
}