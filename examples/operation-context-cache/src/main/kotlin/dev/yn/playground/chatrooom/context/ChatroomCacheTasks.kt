package dev.yn.playground.chatrooom.context

import dev.yn.playground.chatrooom.models.ChatroomId
import dev.yn.playground.chatrooom.sql.query.GetChatRoom
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.ex.query
import dev.yn.playground.Task

object ChatroomCacheTasks {
    fun <I: ChatroomId> hydrateExistingChatroom(): Task<I, I, ApplicationContext<ChatroomOperationCache>> =
            Task.updateContext(
                    Task.identity<I, ApplicationContext<ChatroomOperationCache>>()
                            .map { it.getChatroomId() }
                            .query(GetChatRoom)
                            .mapWithContext { chatroom, context -> context.cache.useChatroom(chatroom) })

    fun hydrateExistingChatroomById(): Task<String, String, ApplicationContext<ChatroomOperationCache>> =
            Task.updateContext(
                    Task.identity<String, ApplicationContext<ChatroomOperationCache>>()
                            .query(GetChatRoom)
                            .mapWithContext { chatroom, context -> context.cache.useChatroom(chatroom) })
}