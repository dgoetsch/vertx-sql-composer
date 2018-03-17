package dev.yn.playground.context

import dev.yn.playground.serialize.SerializeTaskExecutor
import dev.yn.playground.result.AsyncResult

interface SerializeTaskContextProvider<E: SerializeTaskExecutor> {
    fun getSerializeTaskExecutor(): AsyncResult<E>
}

interface SerializeTaskContext<E: SerializeTaskExecutor> {
    fun getSerializeTaskExecutor(): E
}