package dev.yn.playground.context

import dev.yn.playground.result.AsyncResult

interface ContextProvider<C> {
    fun provideContext(): AsyncResult<C>
}

interface CacheContextProvider<C, R> {
    fun provideContext(r: R): AsyncResult<C>
}