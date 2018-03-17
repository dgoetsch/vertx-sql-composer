package dev.yn.playground.task

import dev.yn.playground.Task
import dev.yn.playground.andThen
import dev.yn.playground.result.AsyncResult
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.funktionale.either.Either

class TaskSpec: StringSpec() {
    init {
        "a task can have no context" {
            val task = Task.map<Int, String, Unit> { it.toString() }
            task.run(10, Unit) shouldBe AsyncResult.succeeded("10")
        }

        "a task can be chained" {
            val task = Task
                    .map<Int, String, Unit> { it.toString() }
                    .map { it.toLong() * 2 }
            task.run(10, Unit) shouldBe AsyncResult.succeeded(20L)
        }

        "a task context can provide properties" {
            data class Config(val appName: String)
            data class ConfigContext(val config: Config)

            val task =
                    Task.map<Int, String, ConfigContext>{ it.toString() }
                            .mapWithContext { i, c -> "Application ${c.config.appName} received $i" }
            task.run(10, ConfigContext(Config("Example"))) shouldBe AsyncResult.succeeded("Application Example received 10")
        }

        "A task can branch based on the result of a task" {
            val double = Task.map<Int, Int, Unit> { it * 2 }
            val half = Task.map<Int, Int, Unit> { it / 2 }
            val rightIfGreaterThanTen = Task.map<Int, Either<Int, Int>, Unit> {
                if(it > 10) {
                    Either.right(it)
                } else {
                    Either.left(it)
                }
            }

            val task = Task.branch(rightIfGreaterThanTen)
                    .left(double)
                    .right(half)

            task.run(5, Unit) shouldBe AsyncResult.succeeded(10)
            task.run(16, Unit) shouldBe AsyncResult.succeeded(8)
        }

        "A task can transform and branch based on the result of a task" {
            val double: Task<Double, Double, Unit> = Task.map { it * 2 }
            val stringLength = Task.map<String, Int, Unit> { it.length }
            fun toLong(): Task<Number, Long, Unit> = Task.map { it.toLong() }

            val rightIfGreaterThanTen = Task.map<Int, Either<Double, String>, Unit> {
                if(it > 10) {
                    Either.right(it.toString())
                } else {
                    Either.left(it.toDouble())
                }
            }

            val task = Task.branch(rightIfGreaterThanTen)
                    .left(double.andThen(toLong()))
                    .right(stringLength.andThen(toLong()))

            task.run(5, Unit) shouldBe AsyncResult.succeeded(10L)
            task.run(16, Unit) shouldBe AsyncResult.succeeded(2L)
        }
    }
}