package dev.yn.playground.consumer.alpha

import com.rabbitmq.client.*
import dev.yn.playground.context.ContextProvider
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import dev.yn.playground.result.Result
import dev.yn.playground.result.toAsyncResult
import org.funktionale.tries.Try
import java.util.concurrent.LinkedBlockingQueue

class AMQPConsumerExecutorProvider(val amqpConnection: Connection): ConsumerExecutorProvider {
    override fun <C> buildExecutor(target: String, contextProvider: ContextProvider<C>): ConsumerExecutor<C> {
        return AMQPConsumerExecutor(amqpConnection, contextProvider, target)
    }
}

class AMQPConsumerExecutor<C>(
        val amqpConnection: Connection,
        val contextProvider: ContextProvider<C>,
        val queue: String): ConsumerExecutor<C> {

    override fun <O> sink(task: Task<ConsumedMessage, O, C>): AsyncResult<Sink> {
        return Try {
            AMQPSink(amqpConnection.createChannel(), queue, task, contextProvider)
        }
                .toAsyncResult()
                .map { it as Sink }
    }

    override fun <O> stream(task: Task<ConsumedMessage, O, C>): AsyncResult<Stream<O>> {
        val result = Try {
            val stream = AMQPStream(
                    amqpConnection.createChannel(),
                    queue,
                    task,
                    contextProvider)
            stream
        }
                .toAsyncResult()
                .map { it as Stream<O> }
        return result
    }
}

abstract class AMQPConsumer<O, C>(
        val channel: Channel,
        val queue: String,
        val task: Task<ConsumedMessage, O, C>,
        val provider: ContextProvider<C>): Consumer {
    private val result = CompletableResult<Unit>()
    private val consumerTag: String

    init {
        consumerTag = channel.basicConsume(queue, false, object: DefaultConsumer(channel) {
            override fun handleDelivery(
                    consumerTag: String,
                    envelope: Envelope,
                    properties: AMQP.BasicProperties,
                    body: ByteArray) {
                handleMessage(consumerTag, envelope, properties, body)
            }
        })
    }

    abstract fun handleMessage(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: ByteArray)

    override fun isRunning(): Boolean {
        return !result.isComplete()
    }

    override fun stop(): AsyncResult<Unit> {
        if(!result.isComplete()) {
            Try { channel.basicCancel(consumerTag) }
                    .onFailure { channel.close() }
                    .map { channel.close() }
                    .onFailure { result.fail(it) }
                    .onSuccess { result.succeed(it) }
        }
        return result
    }

    override fun result(): AsyncResult<Unit> {
        return result
    }
}
class AMQPSink<O, C>(
        channel: Channel,
        queue: String,
        task: Task<ConsumedMessage, O, C>,
        provider: ContextProvider<C>): Sink, AMQPConsumer<O, C>(channel, queue, task, provider) {
    override fun handleMessage(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: ByteArray) {
        provider.provideContext()
                .flatMap { context -> task.run(ConsumedMessage(envelope.routingKey, body), context) }
                .map { channel.basicAck(envelope.deliveryTag, false) }
                .recover {
                    Try {
                        channel.basicNack(envelope.deliveryTag, false, true)
                    }.toAsyncResult()
                }
    }

}

class AMQPStream<O, C>(
        channel: Channel,
        queue: String,
        task: Task<ConsumedMessage, O, C>,
        provider: ContextProvider<C>): Stream<O>, AMQPConsumer<O, C>(channel, queue, task, provider) {
    override fun handleMessage(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: ByteArray) {
        provider.provideContext()
                .flatMap { context -> task.run(ConsumedMessage(envelope.routingKey, body), context) }
                .enqueue()
                .map { channel.basicAck(envelope.deliveryTag, false) }
                .recover {
                    Try {
                        channel.basicNack(envelope.deliveryTag, false, true)
                    }.toAsyncResult()
                }
    }

    private val results = LinkedBlockingQueue<Result<O>> ()

    override fun hasNext(): Boolean {
        return results.isNotEmpty()
    }

    override fun next(): Result<O> {
        return results.poll()
                ?.let { it }
                ?: Result.Failure(RuntimeException(""))
    }

    private fun AsyncResult<O>.enqueue(): AsyncResult<O> {
        return this
                .map {
                    results.add(Result.Success(it))
                    it
                }
                .recover {
                    results.add(Result.Failure(it))
                    AsyncResult.failed(it)
                }
    }
}
