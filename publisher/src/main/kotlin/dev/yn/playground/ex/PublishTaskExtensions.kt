package dev.yn.playground.ex

import dev.yn.playground.publisher.PublishCommand
import dev.yn.playground.publisher.PublishTask
import dev.yn.playground.context.PublishTaskContext
import dev.yn.playground.Task
import dev.yn.playground.andThen

fun <I, O, C: PublishTaskContext<*>> Task<I, O, C>.publish(mapping: (O) -> PublishCommand.Publish) =
        this.andThen(PublishTask.publish(mapping))