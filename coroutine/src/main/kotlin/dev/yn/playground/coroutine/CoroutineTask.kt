package dev.yn.playground.coroutine

import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import kotlinx.coroutines.experimental.launch

data class CoroutineTask<I, O, C>(val f: suspend (I) -> O): Task<I, O, C> {
    override fun run(i: I, context: C): AsyncResult<O> {
        val asyncResult = CompletableResult<O>()
        launch {
            try {
                val out = f(i)
                asyncResult.succeed(out)
            } catch(e: Throwable) {
                asyncResult.fail(e)
            }
        }
        return asyncResult
    }
}