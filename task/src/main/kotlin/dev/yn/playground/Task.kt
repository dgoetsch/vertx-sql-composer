package dev.yn.playground

import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.CompletableResult
import org.funktionale.either.Either
import org.funktionale.tries.Try

fun <I, O, O2, C: CP, CP> Task<I, O, C>.andThen(task: Task<O, O2, CP>): Task<I, O2, C> {
    return this.flatMap(Task.Wrapped<O, O2, C, CP>(task))
}

interface Task<in I, O, C> {
    fun run(i: I, context: C): AsyncResult<O>

    companion object {
        fun <I, C> identity(): Task<I, I, C> = map { it }
        fun <I, O, C> mapWithContext(mapper: (I, C) -> O): Task<I, O, C> = Map(mapper)
        fun <I, O, C> map(mapper: (I) -> O): Task<I, O, C> = mapWithContext({ o, context -> mapper(o) })
        fun <I, O, C> mapTryWithContext(mapper: (I, C) -> Try<O>): Task<I, O, C> = MapTry(mapper)
        fun <I, O, C> mapTry(mapper: (I) -> Try<O>): Task<I, O, C> = mapTryWithContext({ o, context -> mapper(o) })
        fun <I, J, K, C, O> branch(control: Task<I, Either<J, K>, C>, left: Task<J, O, C>, right: Task<K, O, C>): Task<I, O, C> = Branch(control, left, right)
        fun <I, J, K, C> branch(control: Task<I, Either<J, K>, C>): Branch.Builder<I, J, K, C> = Branch.Builder.control(control)
        fun <I, C> updateContext(task: Task<I, Unit, C>): Task<I, I, C> = UpdateContext(task)
    }

//    fun <O2> andThen(task: Task<O, O2, C>): Task<I, O2, C> = TaskLink(this, task)
    fun <O2> flatMap(task: Task<O, O2, C>): Task<I, O2, C> = TaskLink(this, task)

    fun <O2> mapWithContext(mapper: (O, C) -> O2): Task<I, O2, C> = this.flatMap(Map(mapper))
    fun <O2> map(mapper: (O) -> O2): Task<I, O2, C> = this.mapWithContext({ o, context -> mapper(o) })

    fun <O2> mapTryWithContext(mapper: (O, C) -> Try<O2>): Task<I, O2, C> = this.flatMap(MapTry(mapper))
    fun <O2> mapTry(mapper: (O) -> Try<O2>): Task<I, O2, C> = this.mapTryWithContext({ o, context -> mapper(o) })

    fun updateContext(task: Task<O, Unit, C>): Task<I, O, C> = this.flatMap(UpdateContext(task))

    fun <J, K, O2> branch(control: Task<O, Either<J, K>, C>, left: Task<J, O2, C>, right: Task<K, O2, C>): Task<I, O2, C> =
        this.flatMap(Branch(control, left, right))
        
    fun <J, O2>whenRight(doOptionally: Task<J, O2, C>, control: Task<O, Either<O2, J>, C>): Task<I, O2, C> =
        this.flatMap(Branch(control, identity(), doOptionally))

    fun <J>whenNonNull(doOptionally: Task<J, O, C>, control: Task<O, J?, C>): Task<I, O, C> =
            this.flatMap(TaskWhenNonNull(doOptionally, control))

    fun whenTrue(doOptionally: Task<O, O, C>, control: Task<O, Boolean, C>): Task<I, O, C> =
            this.flatMap(TaskWhenTrue(doOptionally, control))

    data class Wrapped<I, O, C: CP, CP>(val task: Task<I, O, CP>): Task<I, O, C> {
        override fun run(i: I, context: C): AsyncResult<O> {
            return task.run(i, context)
        }
    }
    /**
     * A link that contains two tasks that have been chained together.  A chain is essentially a single linked list of tasks.
     */
    data class TaskLink<I, J, O, C>(val head: Task<I, J, C>, val tail: Task<J, O, C>): Task<I, O, C> {
        override fun run(i: I, context: C): AsyncResult<O> {
            return head.run(i, context).flatMap { tail.run(it, context) }
        }

        override fun <O2> flatMap(task: Task<O, O2, C>): Task<I, O2, C> = TaskLink(this.head, tail.flatMap(task))
    }

    data class Map<I, O, C>(val mapper: (I, C) -> O): Task<I, O, C> {
        override fun run(i: I, context: C): AsyncResult<O> {
            val tri = Try { mapper(i, context) }
            return when(tri) {
                is Try.Failure -> CompletableResult.failed(tri.throwable)
                is Try.Success -> CompletableResult.succeeded(tri.get())
            }
        }
    }

    data class MapTry<I, O, C>(val mapper: (I, C) -> Try<O>): Task<I, O, C> {
        override fun run(i: I, context: C): AsyncResult<O> {
            val tri = mapper(i, context)
            return when(tri) {
                is Try.Failure -> CompletableResult.failed(tri.throwable)
                is Try.Success -> CompletableResult.succeeded(tri.get())
            }
        }
    }

    data class Branch<I, J, K, O, C>(val control: Task<I, Either<J, K>, C>, val left: Task<J, O, C>, val right: Task<K, O, C>): Task<I, O, C> {
        override fun run(i: I, context: C): AsyncResult<O> {
            return control.run(i, context)
                    .flatMap { when(it) {
                        is Either.Left -> left.run(it.l, context)
                        is Either.Right -> right.run(it.r, context)
                    } }
        }

        sealed class Builder<I, J, K, C> {
            abstract val control: Task<I, Either<J, K>, C>

            companion object {
                fun <I, J, K, C> control(control: Task<I, Either<J, K>, C>): Builder<I, J, K, C> = Impl(control)
            }

            fun <O> left(left: Task<J, O, C>): Left<I, J, K, O, C> = Left(control, left)
            fun <O> right(right: Task<K, O, C>): Right<I, J, K, O, C> = Right(control, right)
            fun <O> ifLeft(left: Task<J, O, C>): Left<I, J, K, O, C> = left(left)
            fun <O> ifRight(right: Task<K, O, C>): Right<I, J, K, O, C> = right(right)

            private data class Impl<I, J, K, C>(override val control: Task<I, Either<J, K>, C>): Builder<I, J, K, C>()

            data class Left<I, J, K, O, C>(val control: Task<I, Either<J, K>, C>, val left: Task<J, O, C>) {
                fun right(right: Task<K, O, C>): Task<I, O, C> = Branch(control, left, right)
                fun ifRight(right: Task<K, O, C>): Task<I, O, C> = right(right)
            }
            
            data class Right<I, J, K, O, C>(val control: Task<I, Either<J, K>, C>, val right: Task<K, O, C>) {
                fun left(left: Task<J, O, C>): Task<I, O, C> = Branch(control, left, right)
                fun ifLeft(left: Task<J, O, C>): Task<I, O, C> = left(left)
            }
        }
    }

    data class TaskWhenNonNull<I, J, C>(val doOptionally: Task<J, I, C>, val control: Task<I, J?, C>): Task<I, I, C> {
        override fun run(i: I, context: C): AsyncResult<I> {
            return control.run(i, context)
                    .flatMap {
                        when(it) {
                            null -> CompletableResult.succeeded(i)
                            else -> doOptionally.run(it, context)
                        }
                    }
        }
    }

    data class TaskWhenTrue<I, C>(val doOptionally: Task<I, I, C>, val control: Task<I, Boolean, C>): Task<I, I, C> {
        override fun run(i: I, context: C): AsyncResult<I> {
            return control.run(i, context)
                    .flatMap {
                        when(it) {
                            true -> doOptionally.run(i, context)
                            else -> CompletableResult.succeeded(i)
                        }
                    }
        }
    }

    data class UpdateContext<I, C>(val task: Task<I, Unit, C>): Task<I, I, C> {
        override fun run(i: I, context: C): AsyncResult<I> {
            return task.run(i, context)
                    .map { i }
        }

    }
}