package dev.yn.playground.publisher

import dev.yn.playground.context.PublishTaskContextProvider
import dev.yn.playground.result.AsyncResult
import io.vertx.core.Vertx

class VertxPublishTaskContextProvider(val vertx: Vertx): PublishTaskContextProvider<VertxPublishExecutor> {
    override fun getPublishExecutor(): AsyncResult<VertxPublishExecutor> {
        return AsyncResult.succeeded(VertxPublishExecutor(vertx.eventBus()))
    }

}