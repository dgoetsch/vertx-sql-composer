package dev.yn.playground.serialize

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.yn.playground.context.SerializeTaskContextProvider
import dev.yn.playground.result.AsyncResult
import dev.yn.playground.result.toAsyncResult
import org.funktionale.tries.Try

class JacksonSerializeTaskExecutor(val mapper: ObjectMapper): SerializeTaskExecutor {

    
    override fun <T> serialize(command: SerializeCommand.Serialize<T>): AsyncResult<ByteArray> {
        return Try { mapper.writeValueAsBytes(command.value) }
                .toAsyncResult()
    }

    override fun <T> deserialize(command: SerializeCommand.Deserialize<T>): AsyncResult<T> {
        return Try { mapper.readValue(command.bytes, command.clazz) }
                .toAsyncResult()
    }
}

class JacksonSerializeTaskContextProvider(val objectMapper: ObjectMapper = defaultObjectMapper): SerializeTaskContextProvider<JacksonSerializeTaskExecutor> {
    companion object {
        val defaultObjectMapper by lazy {
            ObjectMapper()
                    .registerModule(KotlinModule())
                    .registerModule(JavaTimeModule())
        }
    }

    override fun getSerializeTaskExecutor(): AsyncResult<JacksonSerializeTaskExecutor> {
        return AsyncResult.succeeded(JacksonSerializeTaskExecutor(objectMapper))
    }
}

