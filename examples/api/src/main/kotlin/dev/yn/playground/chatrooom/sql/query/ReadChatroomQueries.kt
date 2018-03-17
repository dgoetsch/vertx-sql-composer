package dev.yn.playground.chatrooom.sql.query

import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.common.models.Reference
import dev.yn.playground.sql.*
import dev.yn.playground.user.models.UserProfile
import org.funktionale.tries.Try


object GetChatRoom: SQLQueryMapping<String, ChatRoom> {
    val selectChatroom = """
        |SELECT chatroom.id AS id, chatroom.name AS name, chatroom.description AS description,
        |chatroom_permission.permission_key AS permission_key, chatroom_permission.allow_public AS permission_allow_public,
        |NULL AS user_id, NULL AS user_permission_key, NULL AS user_permission_date,
        |NULL AS user_name, NULL AS user_allow_public_message
        |FROM chatroom
        |LEFT JOIN chatroom_permission
        |ON chatroom.id=chatroom_permission.chatroom_id
        |WHERE chatroom.id=?
        |UNION SELECT chatroom.id AS id, chatroom.name AS name, chatroom.description AS description,
        |NULL AS permission_key, NULL AS permission_allow_public,
        |chatroom_user_permission.user_id AS user_id, chatroom_user_permission.permission_key AS user_permission_key, chatroom_user_permission.date_added AS user_permission_date,
        |user_profile.user_name as user_name, user_profile.allow_public_message AS user_allow_public_message
        |FROM chatroom
        |JOIN chatroom_user_permission
        |JOIN user_profile on chatroom_user_permission.user_id = user_profile.id
        |ON chatroom.id = chatroom_user_permission.chatroom_id
        |WHERE chatroom.id=?
    """.trimMargin()

    override fun toSql(i: String): SQLCommand.Query {
        return SQLCommand.Query(SQLStatement.Parameterized(selectChatroom, listOf(i, i)))
    }

    override fun mapResult(i: String, rs: SQLResult.Query): Try<ChatRoom> {
        return Try { rs.result.next() }
                .flatMap(this::parseFirstRow)
                .flatMap { rs.result.asSequence().fold(Try.Success(it)) { agg: Try<ChatRoom>, nextRow: SQLRow -> agg.map { chatRoom ->
                    chatRoom.copy(
                            users = parseUser(nextRow).map { addTo(chatRoom.users, it) }.getOrElse { chatRoom.users },
                            publicPermissions = parsePermission(nextRow).map { chatRoom.publicPermissions.plus(it) }.getOrElse { chatRoom.publicPermissions }
                    )
                }} }
    }

    private fun parseFirstRow(row: SQLRow): Try<ChatRoom> {
        return Try {
            ChatRoom(
                    id = row.getString("id"),
                    name = row.getString("name"),
                    description = row.getString("description"),
                    users = parseUser(row).map(::setOf).getOrElse { emptySet() },
                    publicPermissions = parsePermission(row).map(::setOf).getOrElse { emptySet() })
        }
    }
    private fun parsePermission(row: SQLRow): Try<String> {
        return Try { row.getString("permission_key") }
    }

    private fun parseUser(row: SQLRow): Try<ChatRoomUser> {
        return Try {
            ChatRoomUser(
                    user = Reference.Defined(
                            id = row.getString("user_id"),
                            referenced = UserProfile(
                                    row.getString("user_id"),
                                    row.getString("user_name"),
                                    row.getBoolean("user_allow_public_message"))),
                    chatroom = Reference.Empty(id = row.getString("id")),
                    permissions = setOf(row.getString("user_permission_key")))
        }
    }

    private fun addTo(users: Set<ChatRoomUser>, chatRoomUser: ChatRoomUser):Set<ChatRoomUser> {
        return users.plus(chatRoomUser).fold(emptyMap<String, ChatRoomUser>()) { agg, nextUser ->
            agg.plus(nextUser.user.id to (agg.get(nextUser.user.id)?.let { it.copy(permissions = it.permissions + nextUser.permissions) }?:nextUser))
        }.values.toSet()
    }
}
