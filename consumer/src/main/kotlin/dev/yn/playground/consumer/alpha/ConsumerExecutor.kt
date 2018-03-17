package dev.yn.playground.consumer.alpha

import dev.yn.playground.context.ContextProvider
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
interface ConsumerExecutorProvider {
    fun <C> buildExecutor(target: String, contextProvider: ContextProvider<C>): ConsumerExecutor<C>
}

interface ConsumerExecutor<C> {
    fun <O> sink(task: Task<ConsumedMessage, O, C>): AsyncResult<Sink>
    fun <O> stream(task: Task<ConsumedMessage, O, C>): AsyncResult<Stream<O>>
}
