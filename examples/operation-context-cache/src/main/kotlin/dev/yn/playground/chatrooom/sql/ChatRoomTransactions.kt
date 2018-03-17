package dev.yn.playground.chatrooom.sql
import dev.yn.playground.auth.context.UserSessionCache
import dev.yn.playground.auth.sql.AuthSQLActions
import dev.yn.playground.chatrooom.context.ChatroomCacheTasks
import dev.yn.playground.chatrooom.context.ChatroomOperationCache
import dev.yn.playground.chatrooom.models.*
import dev.yn.playground.chatrooom.sql.query.*
import dev.yn.playground.chatrooom.sql.update.*
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.ex.query
import dev.yn.playground.ex.update
import dev.yn.playground.Task
import dev.yn.playground.andThen
import dev.yn.playground.user.models.UserError
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.funktionale.option.orElse
import org.funktionale.tries.Try

object ChatRoomTransactions {
    private val onlyIfHasUsers: (ChatRoom) -> Try<ChatRoom> = { if(it.users.isEmpty()) Try.Failure(ChatRoomError.NoUsers) else { Try.Success(it) } }

    val addUserTransaction: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomUser, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomUser>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.AddUser))
                    .update(InsertChatRoomUserPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    val removeUserTransaction: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomUser, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomUser>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.RemoveUser))
                    .update(DeleteChatRoomUserPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    val updateChatRoomTransaction: Task<ChatRoom, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoom, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoom>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.Update))
                    .update(UpdateChatRoomFields)
                    .map { it.id }
                    .query(GetChatRoom)

    val getChatRoomTransaction: Task<String, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<String, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroomById())
                    .andThen(authorizeUser(ChatRoomPermissionKey.Get))
                    .mapTryWithContext(this::loadUserFromCache)

    val createChatRoomTransaction: Task<ChatRoom, ChatRoom, ApplicationContext<UserSessionCache>> =
            AuthSQLActions.validate<ChatRoom, UserSessionCache>()
                    .mapTry(onlyIfHasUsers)
                    .update(InsertChatRoom)
                    .update(InsertChatRoomUsers)
                    .update(InsertChatRoomPermissions)

    val addPermissions: Task<ChatRoomPermissions, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomPermissions, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomPermissions>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.AddPublicPermission))
                    .update(AddChatRoomPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    val removePermissions: Task<ChatRoomPermissions, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomPermissions, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomPermissions>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.RemovePublicPermission))
                    .update(DeleteChatRoomPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    val addUserPermissions: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomUser, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomUser>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.AddUserPermission))
                    .update(InsertChatRoomUserPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    val removeUserPermissions: Task<ChatRoomUser, ChatRoom, ApplicationContext<ChatroomOperationCache>> =
            AuthSQLActions.validate<ChatRoomUser, ChatroomOperationCache>()
                    .andThen(ChatroomCacheTasks.hydrateExistingChatroom<ChatRoomUser>())
                    .andThen(authorizeUser(ChatRoomPermissionKey.RemoveUserPermission))
                    .update(DeleteChatRoomUserPermissions)
                    .map { it.chatroom.id }
                    .query(GetChatRoom)

    private fun <I> authorizeUser(chatRoomPermission: ChatRoomPermissionKey): Task<I, I, ApplicationContext<ChatroomOperationCache>> = Task.mapTryWithContext { i, context ->
        context.cache.getChatroom()
                .filter { it.publicPermissions.contains(chatRoomPermission.key) }
                .orElse { context.cache
                        .getChatroom()
                        .flatMap { chatRoom -> context.cache
                                .getUserSession()
                                .flatMap { session -> chatRoom.users
                                        .firstOption {
                                            it.user.id == session.userId
                                                    && it.permissions.contains(chatRoomPermission.key)
                                        }
                                }
                        }
                }
                .map { Try.Success(i) }
                .getOrElse { Try.Failure<I>(UserError.AuthorizationFailed) }
    }

    private fun loadUserFromCache(id: String, context: ApplicationContext<ChatroomOperationCache>): Try<ChatRoom> {
        return context.cache.getChatroom()
                .map { Try.Success(it) }
                .getOrElse { Try.Failure<ChatRoom>(ChatRoomError.NotFound(id)) }
    }

}




