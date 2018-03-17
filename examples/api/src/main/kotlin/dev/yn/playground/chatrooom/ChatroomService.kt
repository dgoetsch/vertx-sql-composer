package dev.yn.playground.chatrooom

import dev.yn.playground.auth.TokenAndInput
import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomPermissions
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.chatrooom.sql.ChatRoomTransactions
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.Task
import devyn.playground.sql.task.SQLTransactionTask

class ChatroomService(val provider: ApplicationContextProvider) {
    val addUserTask: Task<TokenAndInput<ChatRoomUser>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.addUserTransaction)
    val removeUserTask: Task<TokenAndInput<ChatRoomUser>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.removeUserTransaction)
    val addUserPermissionsTask: Task<TokenAndInput<ChatRoomUser>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.addUserPermissions)
    val removeUserPermissionsTask: Task<TokenAndInput<ChatRoomUser>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.removeUserPermissions)
    val addPublicPermissionTask: Task<TokenAndInput<ChatRoomPermissions>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.addPermissions)
    val removePublicPermissionTask: Task<TokenAndInput<ChatRoomPermissions>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.removePermissions)
    val createChatRoomTask: Task<TokenAndInput<ChatRoom>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.createChatRoomTransaction)
    val updateChatRoomTask: Task<TokenAndInput<ChatRoom>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.updateChatRoomTransaction)
    val getChatroomTask: Task<TokenAndInput<String>, ChatRoom, ApplicationContext> = SQLTransactionTask.transaction(ChatRoomTransactions.getChatRoomTransaction)
}