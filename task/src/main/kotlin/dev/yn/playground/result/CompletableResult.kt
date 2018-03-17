package dev.yn.playground.result

import org.funktionale.tries.Try

fun <T> Try<T>.toAsyncResult(): AsyncResult<T> =
    when(this) {
        is Try.Success -> AsyncResult.succeeded(this.get())
        is Try.Failure -> AsyncResult.failed(this.throwable)
    }

interface AsyncResult<T> {
    companion object {
        fun <T> succeeded(t: T): AsyncResult<T> = CompletableResult.succeeded(t)
        fun <T> failed(error: Throwable): AsyncResult<T> = CompletableResult.failed(error)
    }

    fun <U> map(f: (T) -> U): AsyncResult<U>
    fun <U> flatMap(f: (T) -> AsyncResult<U>): AsyncResult<U>
    fun recover(f: (Throwable) -> AsyncResult<T>): AsyncResult<T>

    fun setHandler(handler: (Result<T>) -> Unit)

    fun isComplete(): Boolean
    fun isSuccess(): Boolean
    fun isFailure(): Boolean

    fun result(): T?
    fun error(): Throwable?
}

interface Completable<T> {
    fun succeed(t: T)
    fun fail(error: Throwable)

    fun completionHandler(): ResultHandler<T> = {
        when(it) {
            is Result.Failure -> fail(it.error)
            is Result.Success -> succeed(it.result)
        }
    }
}

interface CompletableResult<T>: AsyncResult<T>, Completable<T> {
    companion object {
        operator fun <T> invoke(): CompletableResult<T> = CompletableResultImpl()
        fun <T> succeeded(t: T): CompletableResult<T> {
            val result = CompletableResultImpl<T>()
            result.succeed(t)
            return result
        }

        fun <T> failed(error: Throwable): CompletableResult<T> {
            val result = CompletableResultImpl<T>()
            result.fail(error)
            return result
        }
    }

    private class CompletableResultImpl<T>: CompletableResult<T> {
        @Volatile private var result: Result<T>? = null
        @Volatile private var handler: ResultHandler<T>? = null

        override fun <U> map(f: (T) -> U): AsyncResult<U> {
            val newResult = CompletableResultImpl<U>()
            setHandler {
                when (it) {
                    is Result.Failure -> newResult.fail(it.error)
                    is Result.Success -> {
                        try {
                            newResult.succeed(f(it.result))
                        } catch(e: Throwable) {
                            newResult.fail(e)
                        }
                    }
                }
            }
            return newResult
        }

        override fun <U> flatMap(f: (T) -> AsyncResult<U>): AsyncResult<U> {
            val newResult: CompletableResult<U> = CompletableResultImpl<U>()
            setHandler {
                when(it) {
                    is Result.Failure -> newResult.fail(it.error)
                    is Result.Success ->
                        f(it.result).setHandler { asyncResult ->
                            when(asyncResult) {
                                is Result.Failure -> newResult.fail(asyncResult.error)
                                is Result.Success -> newResult.succeed(asyncResult.result)
                            }
                        }
                }
            }
            return newResult
        }

        override fun recover(f: (Throwable) -> AsyncResult<T>): AsyncResult<T> {
            val newResult: CompletableResult<T> = invoke()
            setHandler {
                when(it) {
                    is Result.Failure ->
                        f(it.error).setHandler { asyncResult ->
                            when(asyncResult) {
                                is Result.Failure -> newResult.fail(asyncResult.error)
                                is Result.Success -> newResult.succeed(asyncResult.result)
                            }
                        }
                    is Result.Success -> newResult.succeed(it.result)
                }
            }
            return newResult
        }

        //TODO make this a protected method
        override fun setHandler(handler: (Result<T>) -> Unit): Unit = synchronized(this) {
            when {
                this.handler == null -> {
                    this.handler = handler
                    result?.let { handler(it) }
                }
                else -> throw IllegalStateException("Result is already succeed")
            }
        }

        override fun succeed(t: T): Unit = synchronized(this) {
            if(!isComplete()) {
                result = Result.Success(t)
                handler?.invoke(Result.Success(t))
            } else {
                throw IllegalStateException("Result is already succeed")
            }
        }

        override fun fail(error: Throwable): Unit = synchronized(this) {
            if(!isComplete()) {
                result = Result.Failure(error)
                handler?.invoke(Result.Failure(error))
            } else {
                throw IllegalStateException("Result is already succeed")
            }
        }

        override fun isComplete(): Boolean {
            return result != null
        }

        override fun isSuccess(): Boolean {
            return result?.isSuccess?:false
        }

        override fun isFailure(): Boolean {
            return result?.isSuccess?.not()?:false
        }

        override fun result(): T? {
            return result?.result
        }

        override fun error(): Throwable? {
            return result?.error
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CompletableResultImpl<*>) return false

            if (result != other.result) return false
            if (handler != other.handler) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = result?.hashCode() ?: 0
            result1 = 31 * result1 + (handler?.hashCode() ?: 0)
            return result1
        }

        override fun toString(): String {
            return "CompletableResultImpl(result=$result, handler=$handler)"
        }
    }
}