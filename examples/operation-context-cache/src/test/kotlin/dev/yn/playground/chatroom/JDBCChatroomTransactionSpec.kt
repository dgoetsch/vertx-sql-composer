package dev.yn.playground.chatroom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rabbitmq.client.ConnectionFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.yn.playground.amqp.AMQPManager
import dev.yn.playground.context.AMQPPublishTaskContextProvider
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.context.JDBCDataSourceTaskContextProvider
import dev.yn.playground.context.PublishTaskContextProvider
import dev.yn.playground.publisher.PublishTaskExecutor
import dev.yn.playground.context.SerializeTaskContextProvider
import dev.yn.playground.serialize.SerializeTaskExecutor
import dev.yn.playground.serialize.JacksonSerializeTaskContextProvider
import dev.yn.playground.sql.SQLExecutor
import dev.yn.playground.context.SQLTaskContextProvider
import dev.yn.playground.user.JDBCUserServiceSpec

class JDBCChatroomTransactionSpec: ChatroomTransactionsSpec() {

    companion object {
        val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

        val amqpConnectionFactory: ConnectionFactory by lazy {
            AMQPManager.connectionFactory()
        }

        val amqpConnection by lazy {
            AMQPManager.cleanConnection(amqpConnectionFactory)
        }
        val hikariDSConfig: HikariConfig by lazy {
            val config = HikariConfig()
            config.jdbcUrl = "jdbc:postgresql://localhost:5432/chitchat"
            config.username = "chatty_tammy"
            config.password = "gossipy"
            config.driverClassName = "org.postgresql.Driver"
            config.maximumPoolSize = 30
            config.poolName = "test_pool"
            config
        }

        val hikariDataSource = HikariDataSource(hikariDSConfig)
        val sqlConnectionProvider = JDBCDataSourceTaskContextProvider(hikariDataSource) as SQLTaskContextProvider<SQLExecutor>
        val publishContextProvider by lazy { AMQPPublishTaskContextProvider(AMQPManager.amqpExchange, JDBCUserServiceSpec.amqpConnection, AMQPManager.basicProperties) as PublishTaskContextProvider<PublishTaskExecutor> }
        val serializeContextProvider = JacksonSerializeTaskContextProvider(objectMapper) as SerializeTaskContextProvider<SerializeTaskExecutor>

        val provider: ApplicationContextProvider by lazy {
            ApplicationContextProvider(publishContextProvider, sqlConnectionProvider, serializeContextProvider)
        } }

    override fun provider(): ApplicationContextProvider = JDBCChatroomTransactionSpec.provider

    override fun closeResources() {
        hikariDataSource.close()
        amqpConnection.close()
    }
}