package dev.yn.playground.context

import dev.yn.playground.publisher.PublishTaskExecutor
import dev.yn.playground.result.AsyncResult

interface PublishTaskContextProvider<E: PublishTaskExecutor> {
    fun getPublishExecutor(): AsyncResult<E>
}

interface PublishTaskContext<E: PublishTaskExecutor> {
    fun getPublishExecutor(): E
}