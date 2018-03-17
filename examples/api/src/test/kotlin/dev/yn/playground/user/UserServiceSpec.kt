package  dev.yn.playground.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.consumer.alpha.ConsumedMessage
import dev.yn.playground.consumer.alpha.ConsumerExecutorProvider
import dev.yn.playground.consumer.alpha.Stream
import dev.yn.playground.sql.SQLCommand
import dev.yn.playground.sql.SQLError
import dev.yn.playground.sql.SQLStatement
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.Result
import dev.yn.playground.user.extensions.schema.dropUserSchema
import dev.yn.playground.user.extensions.schema.initUserSchema
import dev.yn.playground.user.extensions.transaction.deleteAllUsers
import dev.yn.playground.user.models.*
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.*
import dev.yn.playground.user.sql.EnsureNoSessionExists
import dev.yn.playground.user.sql.UserSQL
import dev.yn.playground.user.sql.ValidatePasswordForUserId
import io.kotlintest.Spec
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldNotBe
import org.funktionale.tries.Try
import org.slf4j.LoggerFactory


abstract class UserServiceSpec : StringSpec() {

    val LOG = LoggerFactory.getLogger(this.javaClass)

    abstract fun provider(): ApplicationContextProvider
    abstract fun consumerExecutorProvider(): ConsumerExecutorProvider
    abstract fun closeResources()

    val userService: UserService = UserService(provider())

    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())
    fun loginConsumer(): Stream<UserSession> {
        return awaitSucceededFuture(
                userLoginConsumer(consumerExecutorProvider(), provider())
                        .stream(Task.identity<ConsumedMessage, ApplicationContext>()
                                .mapTry { Try { objectMapper.readValue(it.body, UserSession::class.java) } }))!!
    }

    fun createConsumer(): Stream<UserProfile> {
        return awaitSucceededFuture(
                dev.yn.playground.user.userCreateConsumer(consumerExecutorProvider(), provider())
                        .stream(Task.identity<ConsumedMessage, ApplicationContext>()
                                .mapTry { Try { objectMapper.readValue(it.body, UserProfile::class.java) } }))!!
    }
    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        awaitSucceededFuture(provider().provideContext().flatMap{ it.dropUserSchema() })
        awaitSucceededFuture(provider().provideContext().flatMap{ it.initUserSchema() })

        spec()
        awaitSucceededFuture(provider().provideContext().flatMap{ it.deleteAllUsers() })
        awaitSucceededFuture(provider().provideContext().flatMap{ it.dropUserSchema() })
        closeResources()
    }

    init {
        "Login a userName" {
            val userId = UUID.randomUUID().toString()
            val password = "pass1"
            val userName = "sally"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)
            val createStream = createConsumer()
            val loginStream= loginConsumer()

            createStream.isRunning() shouldBe true
            loginStream.isRunning() shouldBe true

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))
            session?.userId shouldBe userId
            session shouldNotBe null

            awaitStreamItem(createStream, user)
            awaitStreamItem(loginStream, session!!)
            awaitSucceededFuture(createStream.stop())
            awaitSucceededFuture(loginStream.stop())
        }

        "Fail to loginActionChain a user with a bad password" {
            val userId = UUID.randomUUID().toString()
            val password = "pass2"
            val userName = "sally2"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val expectedError = SQLError.OnCommand(
                    SQLCommand.Query(SQLStatement.Parameterized(ValidatePasswordForUserId.selectUserPassword, listOf(userId, "bad"))),
                    UserError.AuthenticationFailed)
            awaitFailedFuture(
                    userService.loginUser(userAndPassword.copy(password = "bad")),
                    expectedError)
        }

        "Fail to loginActionChain a user who is already logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass3"
            val userName = "sally3"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            awaitSucceededFuture(userService.loginUser(userAndPassword))?.userId shouldBe userId
            val expectedError = SQLError.OnCommand(
                    SQLCommand.Query(SQLStatement.Parameterized(EnsureNoSessionExists.selectUserSessionExists, listOf(userId))),
                    UserError.SessionAlreadyExists(userId))
            awaitFailedFuture(
                    userService.loginUser(userAndPassword.copy(password = password)),
                    expectedError)
        }

        "Allow a user to get themselves once logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass4"
            val userName = "sally4"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))!!

            awaitSucceededFuture(
                    userService.getUser(userId, session.sessionKey),
                    user)
        }

        "Not Allow a user to select another user once logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass5"
            val userName = "sally5"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            val userId2 = UUID.randomUUID().toString()
            val password2 = "pass6"
            val userName2 = "sally6"
            val user2 = UserProfile(userId2, userName2, false)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)
            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user2, password2)),
                    user2)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))!!

            awaitFailedFuture(
                    userService.getUser(userId2, session.sessionKey),
                    SQLError.OnCommand(
                            SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(session.sessionKey))),
                            UserError.AuthenticationFailed))
        }

        "Not Allow a user with a bogus key to select another user" {
            val userId = UUID.randomUUID().toString()
            val password = "pass7"
            val userName = "sally7"
            val user = UserProfile(userId, userName, false)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val sessionKey = UUID.randomUUID().toString()

            awaitFailedFuture(
                    userService.getUser(userId, sessionKey),
                    SQLError.OnCommand(
                            SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(sessionKey))),
                            UserError.AuthenticationFailed))
        }
    }

    fun <T> awaitStreamItem(stream: Stream<T>, expected: T, maxDuration: Long = 1000L) {
        val start = System.currentTimeMillis()
        while(!stream.hasNext() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        val result = stream.next()
        when(result) {
            is Result.Success -> result.result shouldBe expected
            is Result.Failure -> throw result.error
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