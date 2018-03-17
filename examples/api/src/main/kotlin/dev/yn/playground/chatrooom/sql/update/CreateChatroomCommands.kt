package dev.yn.playground.chatrooom.sql.update

import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.sql.*
import org.funktionale.tries.Try

object InsertChatRoom: SQLUpdateMapping<ChatRoom, ChatRoom> {
    override fun toSql(i: ChatRoom): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized("INSERT INTO chatroom (id, name, description) VALUES (?, ?, ?)", listOf(i.id, i.name, i.description)))
    }

    override fun mapResult(i: ChatRoom, rs: SQLResult.Update): Try<ChatRoom> {
        return if(rs.count == 1) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }

}

object InsertChatRoomUsers : SQLUpdateMapping<ChatRoom, ChatRoom> {
    override fun toSql(i: ChatRoom): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "INSERT INTO chatroom_user_permission (chatroom_id, user_id, permission_key, date_added) VALUES ${(1..i.users.map {it.permissions.size }.sum()).map { "(?, ?, ?, transaction_timestamp())" }.joinToString(",")}",
                i.users.flatMap { chatRoomUser ->  chatRoomUser.permissions.flatMap { listOf(chatRoomUser.chatroom.id, chatRoomUser.user.id, it) }}))
    }

    override fun mapResult(i: ChatRoom, rs: SQLResult.Update): Try<ChatRoom> {
        return if(rs.count == i.users.map { it.permissions.size }.sum()) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }

}

object InsertChatRoomPermissions: SQLUpdateMapping<ChatRoom, ChatRoom> {
    override fun toSql(i: ChatRoom): SQLCommand.Update {
        return SQLCommand.Update(SQLStatement.Parameterized(
                "INSERT INTO chatroom_permission (chatroom_id, permission_key, allow_public) VALUES ${i.publicPermissions.map { "(?, ?, true)" }.joinToString(",") }",
                i.publicPermissions.flatMap { listOf(i.id, it)}))
    }

    override fun mapResult(i: ChatRoom, rs: SQLResult.Update): Try<ChatRoom> {
        return if(rs.count == i.publicPermissions.size) Try.Success(i) else Try.Failure(SQLError.UpdateMappingFailed(this, i))
    }
}