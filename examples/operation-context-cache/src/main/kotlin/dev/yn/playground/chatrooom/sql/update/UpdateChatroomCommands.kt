package dev.yn.playground.chatrooom.sql.update

import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomPermissions
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.sql.*
import org.funktionale.tries.Try

object InsertChatRoomUserPermissions : SQLUpdateMapping<ChatRoomUser, ChatRoomUser> {
    override fun toSql(i: ChatRoomUser): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "INSERT INTO chatroom_user_permission (chatroom_id, user_id, permission_key, date_added) VALUES ${(1..i.permissions.size).map {"(?, ?, ?, transaction_timestamp())"}.joinToString(",")}",
                i.permissions.flatMap { listOf(i.chatroom.id, i.user.id, it) }))
    }

    override fun mapResult(i: ChatRoomUser, rs: SQLResult.Update): Try<ChatRoomUser> {
        return if(rs.count == i.permissions.size) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }
}

object DeleteChatRoomUserPermissions : SQLUpdateMapping<ChatRoomUser, ChatRoomUser> {
    override fun toSql(i: ChatRoomUser): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "DELETE FROM chatroom_user_permission WHERE chatroom_id=? AND user_id=? AND (${i.permissions.map { "permission_key = ?"}.joinToString(" OR ")})",
                listOf(i.chatroom.id, i.user.id) + i.permissions))
    }

    override fun mapResult(i: ChatRoomUser, rs: SQLResult.Update): Try<ChatRoomUser> {
        return if(rs.count == i.permissions.size) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }
}

object DeleteChatRoomPermissions: SQLUpdateMapping<ChatRoomPermissions, ChatRoomPermissions> {
    override fun toSql(i: ChatRoomPermissions): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "DELETE FROM chatroom_permission WHERE chatroom_id=? AND (${i.publicPermissions.map { "permission_key = ?"}.joinToString(" OR ")})",
        listOf(i.chatroom.id) + i.publicPermissions))
    }

    override fun mapResult(i: ChatRoomPermissions, rs: SQLResult.Update): Try<ChatRoomPermissions> {
        return if(rs.count== i.publicPermissions.size) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }

}

object UpdateChatRoomFields: SQLUpdateMapping<ChatRoom, ChatRoom> {
    override fun mapResult(i: ChatRoom, rs: SQLResult.Update): Try<ChatRoom> {
        return if(rs.count== 1) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }

    override fun toSql(i: ChatRoom): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "UPDATE chatroom SET name=?, description=? WHERE id=?",
                listOf(i.name, i.description, i.id)))
    }
}

object AddChatRoomPermissions : SQLUpdateMapping<ChatRoomPermissions, ChatRoomPermissions> {
    override fun toSql(i: ChatRoomPermissions): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "INSERT INTO chatroom_permission (chatroom_id, permission_key, allow_public) VALUES ${i.publicPermissions.map { "(?, ?, true)" }.joinToString(",") }",
                i.publicPermissions.flatMap { listOf(i.chatroom.id, it)}))
    }

    override fun mapResult(i: ChatRoomPermissions, rs: SQLResult.Update): Try<ChatRoomPermissions> {
        return if(rs.count == i.publicPermissions.size) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }
}