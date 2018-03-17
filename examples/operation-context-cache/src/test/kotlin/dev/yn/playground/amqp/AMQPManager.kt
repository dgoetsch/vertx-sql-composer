package dev.yn.playground.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import dev.yn.playground.user.userCreatedAddress
import dev.yn.playground.user.userLoginAddress

object AMQPManager {
    val amqpExchange = "events.ex"
    val basicProperties: AMQP.BasicProperties = AMQP.BasicProperties()

    fun connectionFactory(): ConnectionFactory {
        val factory = ConnectionFactory()
        factory.username = "rabbitmq"
        factory.password = "rabbitmq"
        factory.host = "localhost"
        factory.port = 5672
        factory.virtualHost = "/"
        return factory
    }

    fun cleanConnection(factory: ConnectionFactory): Connection {
        val connection = factory.newConnection()
        AMQPManager.destroyAMQPResources(connection)
        AMQPManager.initAMQPResources(connection)
        return connection
    }

    fun initAMQPResources(connection: Connection) {
        val channel = connection.createChannel()
        val exchange = channel.exchangeDeclare(amqpExchange, BuiltinExchangeType.TOPIC)
        val loginQueue = channel.queueDeclare(userLoginAddress, true, false, false, emptyMap())
        val createQueue = channel.queueDeclare(userCreatedAddress, true, false, false, emptyMap())
        channel.queueBind(userLoginAddress, amqpExchange, userLoginAddress)
        channel.queueBind(userCreatedAddress, amqpExchange, userCreatedAddress)
        channel.close()
    }

    fun destroyAMQPResources(connection: Connection) {
        val channel = connection.createChannel()
        channel.queueDelete(userLoginAddress)
        channel.queueDelete(userCreatedAddress)
        channel.exchangeDelete(amqpExchange)
        channel.close()
    }
}