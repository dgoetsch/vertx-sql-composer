package dev.yn.playground.chatrooom.context

import dev.yn.playground.auth.AuthSession
import dev.yn.playground.auth.context.UserSessionCache
import dev.yn.playground.chatrooom.models.ChatRoom
import org.funktionale.option.Option

interface ExistingChatroomCache {
    fun useChatroom(chatroom: ChatRoom)
    fun getChatroom(): Option<ChatRoom>
}

data class ChatroomOperationCache(
        private val sessionKey: String,
        private var session: Option<AuthSession> = Option.None,
        private var chatroom: Option<ChatRoom> = Option.None)
    : UserSessionCache, ExistingChatroomCache {
    override fun useChatroom(chatroom: ChatRoom) {
        this.chatroom = Option.Some(chatroom)
    }

    override fun getChatroom(): Option<ChatRoom> = chatroom

    override fun getUserSessionKey(): String = sessionKey

    override fun setUserSession(userSession: AuthSession) {
        this.session = Option.Some(userSession)
    }

    override fun getUserSession(): Option<AuthSession> = session
}