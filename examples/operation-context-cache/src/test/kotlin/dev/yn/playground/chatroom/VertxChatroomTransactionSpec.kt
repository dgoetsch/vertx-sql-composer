package dev.yn.playground.chatroom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.context.PublishTaskContextProvider
import dev.yn.playground.publisher.PublishTaskExecutor
import dev.yn.playground.context.SerializeTaskContextProvider
import dev.yn.playground.serialize.SerializeTaskExecutor
import dev.yn.playground.serialize.JacksonSerializeTaskContextProvider
import dev.yn.playground.sql.SQLExecutor
import dev.yn.playground.context.SQLTaskContextProvider
import dev.yn.playground.publisher.VertxPublishTaskContextProvider
import dev.yn.playground.sql.VertxSQLTaskContextProvider
import dev.yn.playground.result.VertxResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLClient

class VertxChatroomTransactionSpec: ChatroomTransactionsSpec() {

    companion object {
        val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

        val hikariConfig = JsonObject()
                .put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
                .put("jdbcUrl", "jdbc:postgresql://localhost:5432/chitchat")
                .put("username", "chatty_tammy")
                .put("password", "gossipy")
                .put("driver_class", "org.postgresql.Driver")
                .put("maximumPoolSize", 30)
                .put("poolName", "test_pool")

        val vertx by lazy { Vertx.vertx() }

        val sqlClient: SQLClient by lazy {
            JDBCClient.createShared(vertx, hikariConfig, "test_ds")
        }
        val sqlConnectionProvider = VertxSQLTaskContextProvider(sqlClient) as SQLTaskContextProvider<SQLExecutor>
        val publishContextProvider = VertxPublishTaskContextProvider(vertx) as PublishTaskContextProvider<PublishTaskExecutor>
        val serializeContextProvider = JacksonSerializeTaskContextProvider(objectMapper) as SerializeTaskContextProvider<SerializeTaskExecutor>

        val provider: ApplicationContextProvider by lazy {
            ApplicationContextProvider(publishContextProvider, sqlConnectionProvider, serializeContextProvider)
        }
    }

    override fun provider(): ApplicationContextProvider = VertxChatroomTransactionSpec.provider

    override fun closeResources() {
        val clientF = Future.future<Void>()
        sqlClient.close(clientF.completer())
        awaitSucceededFuture(VertxResult(clientF))
        val future = Future.future<Void>()
        vertx.close(future.completer())
        awaitSucceededFuture(VertxResult(future))

    }
}