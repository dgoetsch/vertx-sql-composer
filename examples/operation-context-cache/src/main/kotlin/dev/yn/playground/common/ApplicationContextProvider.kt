package dev.yn.playground.common

import dev.yn.playground.context.CacheContextProvider
import dev.yn.playground.context.ContextProvider
import dev.yn.playground.context.PublishTaskContext
import dev.yn.playground.context.PublishTaskContextProvider
import dev.yn.playground.publisher.PublishTaskExecutor
import dev.yn.playground.context.SerializeTaskContext
import dev.yn.playground.context.SerializeTaskContextProvider
import dev.yn.playground.serialize.SerializeTaskExecutor
import dev.yn.playground.sql.SQLExecutor
import dev.yn.playground.context.SQLTaskContextProvider
import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult

class ApplicationContextProvider (
        val publishProvider: PublishTaskContextProvider<PublishTaskExecutor>,
        val sqlProvider: SQLTaskContextProvider<SQLExecutor>,
        val serializeProvider: SerializeTaskContextProvider<SerializeTaskExecutor>
): ContextProvider<ApplicationContext<Unit>> {

    override fun provideContext(): AsyncResult<ApplicationContext<Unit>> = provideContext(Unit)

    private fun getPublishExecutor(): AsyncResult<PublishTaskExecutor> {
        return publishProvider.getPublishExecutor()
    }

    private fun getConnection(): AsyncResult<SQLExecutor> = sqlProvider.getConnection()

    private fun getSerializer(): AsyncResult<SerializeTaskExecutor> = serializeProvider.getSerializeTaskExecutor()

    fun <R> provideContext(r: R): AsyncResult<ApplicationContext<R>> {
        return getConnection()
                .flatMap { sqlExecutor ->
                    getPublishExecutor().flatMap { publishExecutor ->
                        getSerializer().map { serializeTaskExecutor ->
                            ApplicationContext(publishExecutor, sqlExecutor, serializeTaskExecutor, r)
                        }
                    }
                }
    }

    fun <I, O, R> runOnContext(task: Task<I, O, ApplicationContext<R>>, i: I, r: R): AsyncResult<O> {
        return provideContext(r)
                .flatMap { task.run(i, it) }
    }
}

class CacheApplicationContextProvider<R>(
        val publishProvider: PublishTaskContextProvider<PublishTaskExecutor>,
        val sqlProvider: SQLTaskContextProvider<SQLExecutor>,
        val serializeProvider: SerializeTaskContextProvider<SerializeTaskExecutor>
): CacheContextProvider<ApplicationContext<R>, R> {

    private fun getPublishExecutor(): AsyncResult<PublishTaskExecutor> {
        return publishProvider.getPublishExecutor()
    }

    private fun getConnection(): AsyncResult<SQLExecutor> = sqlProvider.getConnection()

    private fun getSerializer(): AsyncResult<SerializeTaskExecutor> = serializeProvider.getSerializeTaskExecutor()

    override fun provideContext(r: R): AsyncResult<ApplicationContext<R>> {
        return getConnection()
                .flatMap { sqlExecutor ->
                    getPublishExecutor().flatMap { publishExecutor ->
                        getSerializer().map { serializeTaskExecutor ->
                            ApplicationContext(publishExecutor, sqlExecutor, serializeTaskExecutor, r)
                        }
                    }
                }
    }

}

interface OperationCache<R> {
    fun getOperationCache(): R
}

class ApplicationContext<R>(
        val publishTaskExecutor: PublishTaskExecutor,
        val sqlExecutor: SQLExecutor,
        val serializeExecutor: SerializeTaskExecutor,
        val cache: R):
        PublishTaskContext<PublishTaskExecutor>,
        SQLTaskContext<SQLExecutor>,
        SerializeTaskContext<SerializeTaskExecutor>,
        OperationCache<R>
{
    override fun getSerializeTaskExecutor(): SerializeTaskExecutor = serializeExecutor
    override fun getOperationCache(): R = cache
    override fun getPublishExecutor(): PublishTaskExecutor = publishTaskExecutor
    override fun getSQLExecutor(): SQLExecutor = sqlExecutor
}