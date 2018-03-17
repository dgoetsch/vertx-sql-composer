package dev.yn.playground.chatroom

import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.context.PublishSkriptContextProvider
import dev.yn.playground.context.SQLSkriptContextProvider
import dev.yn.playground.context.SerializeSkriptContextProvider
import dev.yn.playground.context.VertxSerializeSkriptContextProvider
import dev.yn.playground.publisher.PublishSkriptExecutor
import dev.yn.playground.publisher.VertxPublishSkriptContextProvider
import dev.yn.playground.result.VertxResult
import dev.yn.playground.serialize.SerializeSkriptExecutor
import dev.yn.playground.sql.SQLExecutor
import dev.yn.playground.sql.VertxSQLSkriptContextProvider
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLClient

class VertxChatroomTransactionSpec: ChatroomTransactionsSpec() {

    companion object {
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
        val sqlConnectionProvider = VertxSQLSkriptContextProvider(sqlClient) as SQLSkriptContextProvider<SQLExecutor>
        val publishContextProvider = VertxPublishSkriptContextProvider(vertx) as PublishSkriptContextProvider<PublishSkriptExecutor>
        val serializeContextProvider = VertxSerializeSkriptContextProvider() as SerializeSkriptContextProvider<SerializeSkriptExecutor>

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