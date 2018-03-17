package dev.yn.playground.chatrooom

import dev.yn.playground.auth.context.UserSessionCache
import dev.yn.playground.chatrooom.context.ChatroomOperationCache
import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomPermissions
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.chatrooom.sql.ChatRoomTransactions
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import devyn.playground.sql.task.SQLTransactionTask

class ChatRoomService(val provider: ApplicationContextProvider) {
    companion object {
        val addUserTask: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.addUserTransaction)
        val removeUserTask: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.removeUserTransaction)
        val addUserPermissionsTask: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.addUserPermissions)
        val removeUserPermissionsTask: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.removeUserPermissions)
        val addPublicPermissionTask: Task<ChatRoomPermissions, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.addPermissions)
        val removePublicPermissionTask: Task<ChatRoomPermissions, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.removePermissions)
        val createChatRoomTask: Task<ChatRoom, ChatRoom, ApplicationContext<UserSessionCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.createChatRoomTransaction)
        val updateChatRoomTask: Task<ChatRoom, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.updateChatRoomTransaction)
        val getChatroomTask: Task<String, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
                SQLTransactionTask.transaction(ChatRoomTransactions.getChatRoomTransaction)
    }

    fun addUser(chatRoomUser: ChatRoomUser, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(addUserTask, chatRoomUser, ChatroomOperationCache(sessionKey))

    fun removeUser(chatRoomUser: ChatRoomUser, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(removeUserTask, chatRoomUser, ChatroomOperationCache(sessionKey))

    fun addUserPermissions(chatRoomUser: ChatRoomUser, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(addUserPermissionsTask, chatRoomUser, ChatroomOperationCache(sessionKey))

    fun removeUserPermissions(chatRoomUser: ChatRoomUser, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(removeUserPermissionsTask, chatRoomUser, ChatroomOperationCache(sessionKey))

    fun addPublicPermissions(chatRoomPermissions: ChatRoomPermissions, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(addPublicPermissionTask, chatRoomPermissions, ChatroomOperationCache(sessionKey))

    fun removePublicPermissions(chatRoomPermissions: ChatRoomPermissions, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(removePublicPermissionTask, chatRoomPermissions, ChatroomOperationCache(sessionKey))

    fun createChatRoom(chatRoom: ChatRoom, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(createChatRoomTask, chatRoom, ChatroomOperationCache(sessionKey))

    fun updateChatRoom(chatRoom: ChatRoom, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(updateChatRoomTask, chatRoom, ChatroomOperationCache(sessionKey))

    fun getChatRoom(chatRoomId: String, sessionKey: String): AsyncResult<ChatRoom> =
            provider.runOnContext(getChatroomTask, chatRoomId, ChatroomOperationCache(sessionKey))

}