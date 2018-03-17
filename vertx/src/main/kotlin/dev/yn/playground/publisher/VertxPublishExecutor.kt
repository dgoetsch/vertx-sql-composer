package dev.yn.playground.publisher

import dev.yn.playground.result.AsyncResult
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import org.funktionale.tries.Try

class VertxPublishExecutor(val eventBus: EventBus): PublishTaskExecutor {
    override fun publish(command: PublishCommand.Publish): AsyncResult<Unit> {
        val publishResult = Try { eventBus.publish(command.target, Buffer.buffer(command.body)) }
        return when(publishResult) {
            is Try.Success -> AsyncResult.succeeded(Unit)
            is Try.Failure -> AsyncResult.failed(publishResult.throwable)
        }
    }
}