package dev.yn.playground.chatroom

import dev.yn.playground.chatrooom.ChatRoomService
import dev.yn.playground.chatrooom.models.ChatRoom
import dev.yn.playground.chatrooom.models.ChatRoomPermissionKey
import dev.yn.playground.chatrooom.models.ChatRoomPermissions
import dev.yn.playground.chatrooom.models.ChatRoomUser
import dev.yn.playground.chatrooom.sql.ChatRoomSchema
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.common.models.Reference
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.user.models.UserError
import dev.yn.playground.user.UserFixture
import dev.yn.playground.user.UserService
import dev.yn.playground.user.models.UserNameAndPassword
import dev.yn.playground.user.extensions.schema.dropUserSchema
import dev.yn.playground.user.extensions.schema.initUserSchema
import devyn.playground.sql.task.SQLTransactionTask
import io.kotlintest.Spec
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.slf4j.LoggerFactory

abstract class ChatroomTransactionsSpec : StringSpec() {

    val LOG = LoggerFactory.getLogger(this.javaClass)

    abstract fun provider(): ApplicationContextProvider
    val userService = UserService(provider())
    val chatRoomService = ChatRoomService(provider())

    abstract fun closeResources()

    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        awaitSucceededFuture(provider().runOnContext(
                SQLTransactionTask.transaction<Unit, Unit, ApplicationContext<Unit>>(ChatRoomSchema.dropAllAction),
                Unit,
                Unit))
        awaitSucceededFuture(provider().provideContext().flatMap { it.dropUserSchema() })
        awaitSucceededFuture(provider().provideContext().flatMap { it.initUserSchema() })
        awaitSucceededFuture(provider().runOnContext(
                SQLTransactionTask.transaction<Unit, Unit, ApplicationContext<Unit>>(ChatRoomSchema.initAction),
                Unit,
                Unit))
        spec()
        awaitSucceededFuture(provider().runOnContext(
                SQLTransactionTask.transaction<Unit, Unit, ApplicationContext<Unit>>(ChatRoomSchema.dropAllAction),
                Unit,
                Unit))
        awaitSucceededFuture(provider().provideContext().flatMap { it.dropUserSchema() })
        closeResources()
    }

    init {
        "create a chatroom" {
            val user1 = UserFixture.generateUser(1)
            val user2 = UserFixture.generateUser(2)
            awaitSucceededFuture(userService.createUser(user1), user1.userProfile)
            awaitSucceededFuture(userService.createUser(user2), user2.userProfile)

            val session = awaitSucceededFuture(userService.loginUser(UserNameAndPassword(user1.userProfile.name, user1.password)))!!

            val chatRoomId = "chatId"
            val chatRoom = ChatRoom(
                    chatRoomId,
                    "name",
                    "A chat room is described",
                    setOf(
                            ChatRoomUser(
                                    Reference.Defined(user1.userProfile.id, user1.userProfile),
                                    Reference.Empty(chatRoomId),
                                    setOf(ChatRoomPermissionKey.AddUser.key,
                                            ChatRoomPermissionKey.RemoveUser.key,
                                            ChatRoomPermissionKey.AddPublicPermission.key,
                                            ChatRoomPermissionKey.RemovePublicPermission.key,
                                            ChatRoomPermissionKey.Update.key)),
                            ChatRoomUser(
                                    Reference.Defined(user2.userProfile.id, user2.userProfile),
                                    Reference.Empty(chatRoomId),
                                    setOf(ChatRoomPermissionKey.Update.key))),
                    setOf(ChatRoomPermissionKey.Get.key)
            )

            awaitSucceededFuture(chatRoomService.createChatRoom(chatRoom, session.sessionKey), chatRoom)
            awaitSucceededFuture(chatRoomService.getChatRoom(chatRoomId, session.sessionKey), chatRoom)

            val user3 = UserFixture.generateUser(3)
            awaitSucceededFuture(userService.createUser(user3), user3.userProfile)

            val chatRoomWithNewUser = chatRoom.copy(users = chatRoom.users.plus(ChatRoomUser(Reference.Defined(user3.userProfile.id, user3.userProfile), Reference.Empty(chatRoomId), setOf(
                    ChatRoomPermissionKey.Get.key,
                    ChatRoomPermissionKey.Update.key,
                    ChatRoomPermissionKey.AddUserPermission.key,
                    ChatRoomPermissionKey.RemoveUserPermission.key))))
            awaitSucceededFuture(
                    chatRoomService.addUser(
                            ChatRoomUser(
                                    Reference.Defined(user3.userProfile.id, user3.userProfile),
                                    Reference.Empty(chatRoomId),
                                    setOf(
                                            ChatRoomPermissionKey.Get.key,
                                            ChatRoomPermissionKey.Update.key,
                                            ChatRoomPermissionKey.AddUserPermission.key,
                                            ChatRoomPermissionKey.RemoveUserPermission.key)), session.sessionKey),
                            chatRoomWithNewUser)

            val chatRoomAfterDeleteUser = chatRoomWithNewUser.copy(users = chatRoomWithNewUser.users.filterNot { it.user.id == user2.userProfile.id }.toSet())
            awaitSucceededFuture(
                    chatRoomService.removeUser(ChatRoomUser(Reference.Empty(user2.userProfile.id), Reference.Empty(chatRoomId), setOf(ChatRoomPermissionKey.Update.key)), session.sessionKey),
                    chatRoomAfterDeleteUser)

            awaitSucceededFuture(
                    chatRoomService.addPublicPermissions(ChatRoomPermissions(Reference.Empty(chatRoomId), setOf(ChatRoomPermissionKey.AddUser.key)), session.sessionKey),
                    chatRoomAfterDeleteUser.copy(publicPermissions = chatRoomAfterDeleteUser.publicPermissions.plus(ChatRoomPermissionKey.AddUser.key)))

            val nonPublicChatroom = chatRoomAfterDeleteUser.copy(publicPermissions = emptySet())

            awaitSucceededFuture(
                    chatRoomService.removePublicPermissions(ChatRoomPermissions(Reference.Empty(chatRoomId), setOf(ChatRoomPermissionKey.Get.key, ChatRoomPermissionKey.AddUser.key)), session.sessionKey),
                    nonPublicChatroom)

            val updatedChatroom = nonPublicChatroom.copy(name = "upname", description = "chatscription")
            awaitSucceededFuture(
                    chatRoomService.updateChatRoom(updatedChatroom, session.sessionKey),
                    updatedChatroom
            )

            val session2 = awaitSucceededFuture(userService.loginUser(UserNameAndPassword(user3.userProfile.name, user3.password)))!!

            val chatRoomWithUser1AddedPermissions = updatedChatroom.copy(
                    users = updatedChatroom.users.map {
                        if(it.user.id == user1.userProfile.id) it.copy(permissions = it.permissions.plus(
                                setOf(
                                        ChatRoomPermissionKey.AddUserPermission.key,
                                        ChatRoomPermissionKey.RemoveUserPermission.key)))
                        else it
                    }.toSet())

            awaitFailedFuture(
                    chatRoomService.addUserPermissions(
                            ChatRoomUser(Reference.Empty(user1.userProfile.id), Reference.Empty(chatRoomId), setOf(
                                    ChatRoomPermissionKey.AddUserPermission.key,
                                    ChatRoomPermissionKey.RemoveUserPermission.key)),
                            session.sessionKey),
                    UserError.AuthorizationFailed)

            awaitSucceededFuture(
                    chatRoomService.addUserPermissions(
                            ChatRoomUser(Reference.Empty(user1.userProfile.id), Reference.Empty(chatRoomId), setOf(
                                    ChatRoomPermissionKey.AddUserPermission.key,
                                    ChatRoomPermissionKey.RemoveUserPermission.key)),
                            session2.sessionKey),
                    chatRoomWithUser1AddedPermissions)

            val chatRoomWithUser3RemovedPermissions =
                    chatRoomWithUser1AddedPermissions.copy(
                            users = chatRoomWithUser1AddedPermissions.users.map {
                                if(it.user.id == user3.userProfile.id) {
                                    it.copy(permissions = setOf(
                                            ChatRoomPermissionKey.Get.key,
                                            ChatRoomPermissionKey.Update.key))
                                }
                                else it
                            }.toSet())

            awaitSucceededFuture(
                    chatRoomService.removeUserPermissions(
                            ChatRoomUser(
                                    Reference.Empty(user3.userProfile.id),
                                    Reference.Empty(chatRoomId),
                                    setOf(
                                            ChatRoomPermissionKey.AddUserPermission.key,
                                            ChatRoomPermissionKey.RemoveUserPermission.key)),
                            session.sessionKey),
                    chatRoomWithUser3RemovedPermissions)
        }
    }
    fun <T> awaitSucceededFuture(future: AsyncResult<T>, result: T? = null, maxDuration: Long = 1000L): T? {
        val start = System.currentTimeMillis()
        while(!future.isComplete() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        if(!future.isComplete()) fail("Timeout")
        if(future.isFailure()) LOG.error("Expected Success", future.error())
        future.isSuccess() shouldBe true
        result?.let { future.result() shouldBe it }
        return future.result()
    }

    fun <T> awaitFailedFuture(future: AsyncResult<T>, cause: Throwable? = null, maxDuration: Long = 1000L): Throwable? {
        val start = System.currentTimeMillis()
        while(!future.isComplete() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        future.isFailure() shouldBe true
        cause?.let { future.error() shouldBe it}
        return future.error()
    }



}