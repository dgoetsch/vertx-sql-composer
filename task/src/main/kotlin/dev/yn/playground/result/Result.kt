package dev.yn.playground.result

sealed class Result<out T> {
    abstract val result: T?
    abstract val error: Throwable?
    abstract val isSuccess: Boolean

    data class Success<T>(override val result: T): Result<T>() {
        override val isSuccess: Boolean = true
        override val error: Throwable? = null
    }

    data class Failure(override val error: Throwable): Result<Nothing>() {
        override val isSuccess: Boolean = false
        override val result: Nothing? = null
    }
}