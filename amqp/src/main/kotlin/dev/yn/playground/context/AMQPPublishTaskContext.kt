package dev.yn.playground.context

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import dev.yn.playground.publisher.AMQPPublishTaskExecutor
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.toAsyncResult
import org.funktionale.tries.Try

class AMQPPublishTaskContextProvider(
        val exchange: String,
        val connection: Connection,
        val basicProperties: AMQP.BasicProperties): PublishTaskContextProvider<AMQPPublishTaskExecutor> {
    override fun getPublishExecutor(): AsyncResult<AMQPPublishTaskExecutor> {
        return Try { AMQPPublishTaskExecutor(exchange, connection.createChannel(), basicProperties) }.toAsyncResult()
    }

}