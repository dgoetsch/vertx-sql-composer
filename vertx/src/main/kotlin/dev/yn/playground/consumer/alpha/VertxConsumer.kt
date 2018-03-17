package dev.yn.playground.consumer.alpha

import dev.yn.playground.context.ContextProvider
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import dev.yn.playground.result.Result
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import java.util.concurrent.LinkedBlockingQueue

class VertxConsumerExecutorProvider(val vertx: Vertx): ConsumerExecutorProvider {
    override fun <C> buildExecutor(target: String, contextProvider: ContextProvider<C>): ConsumerExecutor<C> {
        return VertxConsumerExecutor(target, vertx, contextProvider)
    }

}
class VertxConsumerExecutor<C>(
        val address: String,
        val vertx: Vertx,
        val provider: ContextProvider<C>): ConsumerExecutor<C> {

    override fun <O> stream(task: Task<ConsumedMessage, O, C>): AsyncResult<Stream<O>> {
        return AsyncResult.succeeded(VertxConsumeStream(vertx, address, task, provider))
    }


    override fun <O> sink(task: Task<ConsumedMessage, O, C>): AsyncResult<Sink> {
        return AsyncResult.succeeded(VertxConsumeSink(vertx, address, task, provider))
    }
}

abstract class VertxConsumer<O, C>(
        val vertx: Vertx,
        val address: String,
        val task: Task<ConsumedMessage, O, C>,
        val provider: ContextProvider<C>): Consumer {
    private val result: CompletableResult<Unit> = CompletableResult()
    private val verticle: AbstractVerticle

    init {
        val handler = this::handleMessage
        verticle = object: AbstractVerticle() {
            override fun toString(): String = "VertxConsumer-$address"

            override fun start() {
                vertx.eventBus().consumer<Buffer>(address, handler)
            }
        }
        vertx.deployVerticle(verticle)
    }

    protected abstract fun handleMessage(message: Message<Buffer>)

    override fun isRunning(): Boolean {
        return !result.isComplete()
    }

    override fun stop(): AsyncResult<Unit> {
        if(!result.isComplete()) {
            vertx.undeploy(
                    verticle.deploymentID(),
                    {
                        if(it.succeeded()) {
                            result.succeed(Unit)
                        } else {
                            result.fail(it.cause())
                        }
                    })
        }
        return result
    }

    override fun result(): AsyncResult<Unit> = result
}

class VertxConsumeSink<O, C>(
        vertx: Vertx,
        address: String,
        task: Task<ConsumedMessage, O, C>,
        provider: ContextProvider<C>): Sink, VertxConsumer<O, C>(vertx, address, task, provider) {

    override fun handleMessage(message: Message<Buffer>) {
        provider.provideContext()
                .flatMap { task.run(ConsumedMessage(address, message.body().bytes), it) }
                .map {
                    message.reply("success") }
                .recover {
                    message.fail(0, it.message)
                    AsyncResult.failed(it)
                }
    }
}

class VertxConsumeStream<O, C>(
        vertx: Vertx,
        address: String,
        task: Task<ConsumedMessage, O, C>,
        provider: ContextProvider<C>): Stream<O>, VertxConsumer<O, C>(vertx, address, task, provider) {


    override fun handleMessage(message: Message<Buffer>) {
        provider.provideContext()
                .flatMap { task.run(ConsumedMessage(address, message.body().bytes), it) }
                .enqueue()
                .map { message.reply("success") }
                .recover {
                    message.fail(0, it.message)
                    AsyncResult.failed(it)
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

