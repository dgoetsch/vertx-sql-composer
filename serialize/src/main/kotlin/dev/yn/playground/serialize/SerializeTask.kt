package dev.yn.playground.serialize

import dev.yn.playground.context.SerializeTaskContext
import dev.yn.playground.Task
import dev.yn.playground.result.AsyncResult

sealed class SerializeTask<I, O>: Task<I, O, SerializeTaskContext<*>> {
    class Serialize<I>: SerializeTask<I, ByteArray>() {
        override fun run(i: I, context: SerializeTaskContext<*>): AsyncResult<ByteArray> {
            return context.getSerializeTaskExecutor()
                    .serialize(SerializeCommand.Serialize(i))
        }
    }

    data class Deserialize<O>(val clazz: Class<O>): SerializeTask<ByteArray, O>() {
        override fun run(i: ByteArray, context: SerializeTaskContext<*>): AsyncResult<O> {
            return context.getSerializeTaskExecutor()
                    .deserialize(SerializeCommand.Deserialize(i, clazz))
        }
    }
}