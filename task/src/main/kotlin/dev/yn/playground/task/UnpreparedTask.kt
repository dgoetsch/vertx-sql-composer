package dev.yn.playground.task

/**
 * A task that needs to have something injected into it with a PROVIDER, for example a vertx instance or a jdbcConnection
 */
interface UnpreparedTask<I, O, PROVIDER> {
    fun prepare(p: PROVIDER): Task<I, O>

    fun <O2> andThen(unpreparedTask: UnpreparedTask<O, O2, PROVIDER>): UnpreparedTask<I, O2, PROVIDER> {
        return UnpreparedTaskLink(this, unpreparedTask)
    }
}

/**
 * A link that contains two unprepared tasks that have been chained together.  A chain is essentially a single linked list of unprepared tasks.
 */
data class UnpreparedTaskLink<I, J, O, PROVIDER>(val unpreparedTask: UnpreparedTask<I, J, PROVIDER>, val next: UnpreparedTask<J, O, PROVIDER>): UnpreparedTask<I, O, PROVIDER> {
    override fun prepare(p: PROVIDER): Task<I, O> {
        return PreparedTaskLink(unpreparedTask.prepare(p), next.prepare(p))
    }

    override fun <O2> andThen(unpreparedTask: UnpreparedTask<O, O2, PROVIDER>): UnpreparedTaskLink<I, J, O2, PROVIDER> {
        return UnpreparedTaskLink(this.unpreparedTask, next.andThen(unpreparedTask))
    }
}