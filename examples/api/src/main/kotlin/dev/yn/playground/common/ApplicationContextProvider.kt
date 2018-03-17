package dev.yn.playground.common

import dev.yn.playground.context.ContextProvider
import dev.yn.playground.context.PublishTaskContext
import dev.yn.playground.context.PublishTaskContextProvider
import dev.yn.playground.publisher.PublishTaskExecutor
import dev.yn.playground.sql.SQLExecutor
import dev.yn.playground.context.SQLTaskContextProvider
import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.user.models.UserSession

class ApplicationContextProvider(
        val publishProvider: PublishTaskContextProvider<PublishTaskExecutor>,
        val sqlProvider: SQLTaskContextProvider<SQLExecutor>
): ContextProvider<ApplicationContext> {

    private fun getPublishExecutor(): AsyncResult<PublishTaskExecutor> {
        return publishProvider.getPublishExecutor()
    }

    private fun getConnection(): AsyncResult<SQLExecutor> = sqlProvider.getConnection()

    override fun provideContext(): AsyncResult<ApplicationContext> {
        return getConnection()
                .flatMap { sqlExecutor ->
                    getPublishExecutor().map { publishExecutor ->
                        ApplicationContext(publishExecutor, sqlExecutor)
                    }
                }
    }

    fun <I, O> runOnContext(task: Task<I, O, ApplicationContext>, i: I): AsyncResult<O> {
        return provideContext()
                .flatMap { task.run(i, it) }
    }
}

class ApplicationContext(val publishTaskExecutor: PublishTaskExecutor, val sqlExecutor: SQLExecutor, val session: UserSession? = null):
        PublishTaskContext<PublishTaskExecutor>,
        SQLTaskContext<SQLExecutor> {
    override fun getPublishExecutor(): PublishTaskExecutor = publishTaskExecutor
    override fun getSQLExecutor(): SQLExecutor = sqlExecutor
}