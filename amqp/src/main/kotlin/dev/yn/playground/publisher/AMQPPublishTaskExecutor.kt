package dev.yn.playground.publisher

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.toAsyncResult
import org.funktionale.tries.Try

class AMQPPublishTaskExecutor(
        val exchange: String,
        val channel: Channel,
        val basicProperties: AMQP.BasicProperties): PublishTaskExecutor {
    override fun publish(command: PublishCommand.Publish): AsyncResult<Unit> {
        return Try {
            channel.basicPublish(exchange, command.target, basicProperties, command.body)
        }.toAsyncResult()
    }
}