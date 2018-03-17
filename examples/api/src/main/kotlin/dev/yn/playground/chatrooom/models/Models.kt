package dev.yn.playground.chatrooom.models

import dev.yn.playground.common.models.Reference
import dev.yn.playground.user.models.UserProfile

data class ChatRoom(
        val id: String,
        val name: String,
        val description: String,
        val users: Set<ChatRoomUser>,
        val publicPermissions: Set<String>
)

data class ChatRoomPermissions(val chatroom: Reference<String, ChatRoom>, val publicPermissions: Set<String>)

data class ChatRoomUser(val user: Reference<String, UserProfile>, val chatroom: Reference<String, ChatRoom>, val permissions: Set<String>)

sealed class ChatRoomPermissionKey {
    val key: String = this.javaClass.simpleName

    object AddUser: ChatRoomPermissionKey()
    object RemoveUser: ChatRoomPermissionKey()
    object AddPublicPermission: ChatRoomPermissionKey()
    object RemovePublicPermission: ChatRoomPermissionKey()
    object AddUserPermission: ChatRoomPermissionKey()
    object RemoveUserPermission: ChatRoomPermissionKey()
    object Update: ChatRoomPermissionKey()
    object Get: ChatRoomPermissionKey()
}

sealed class ChatRoomError: Throwable() {
    object NoUsers: ChatRoomError()
    data class NotFound(val identifier: String): ChatRoomError()
}
