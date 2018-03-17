package dev.yn.playground.sql.task

import dev.yn.playground.context.SQLTaskContext
import dev.yn.playground.Task
import devyn.playground.sql.task.SQLTransactionTask
import io.kotlintest.matchers.shouldBe
import io.kotlintest.mock.mock
import io.kotlintest.specs.StringSpec

class SQLTransactionTaskSpec : StringSpec() {
    init {
        "A transactional task should wrap a SQLTask in a task" {
            val action = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.transaction(action)
            task shouldBe SQLTransactionTask.TransactionalSQLTransactionTask(action)
        }

        "An autocommit task should wrap a SQLTask in a task" {
            val action = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.autoCommit(action)
            task shouldBe SQLTransactionTask.AutoCommitSQlTransactionTask(action)
        }

        "A transactional SQLTransactionTask should map a sqlAction within the transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val action2 = Task.map<String, Long, SQLTaskContext<*>>({ it.toLong() })
            val task = SQLTransactionTask
                    .transaction(action1)
                    .mapInsideTransaction(action2)
            task shouldBe SQLTransactionTask.TransactionalSQLTransactionTask(
                    Task.TaskLink(action1, action2))
        }

        "An autocommit SQLTransactionTask should map a sqlAction within the transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val action2 = Task.map<String, Long, SQLTaskContext<*>>({ it.toLong() })
            val task = SQLTransactionTask
                    .autoCommit(action1)
                    .mapInsideTransaction(action2)
            task shouldBe SQLTransactionTask.autoCommit(
                    Task.TaskLink(
                            action1,
                            action2))
        }

        "A transactional SQLTransactionTask should map a task within the transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val mappedTask = mock<Task<String, Long, SQLTaskContext<*>>>()
            val task = SQLTransactionTask
                    .transaction(action1)
                    .mapInsideTransaction(mappedTask)
            task shouldBe SQLTransactionTask.transaction(
                    Task.TaskLink(
                            action1,
                            mappedTask))
        }

        "An autocommit SQLTransactionTask should map a task within the transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val mappedTask = mock<Task<String, Long, SQLTaskContext<*>>>()
            val task = SQLTransactionTask
                    .autoCommit(action1)
                    .mapInsideTransaction(mappedTask)
            task shouldBe SQLTransactionTask.autoCommit(
                    Task.TaskLink(
                            action1,
                            mappedTask))
        }

        "A transactional SQLTransactionTask should unwrap a SQLTransactionTask mapped within a transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val action2 = Task.map<String, Long, SQLTaskContext<*>>({ it.toLong() })
            val task = SQLTransactionTask
                    .transaction(action1)
                    .mapInsideTransaction(SQLTransactionTask.transaction(action2))
            task shouldBe SQLTransactionTask.transaction(
                    Task.TaskLink(
                            action1,
                            action2))
        }

        "An autocommit SQLTransactionTask should unwrap a SQLTransactionTask mapped within a transaction" {
            val action1 = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val action2 = Task.map<String, Long, SQLTaskContext<*>>({ it.toLong() })
            val task = SQLTransactionTask
                    .autoCommit(action1)
                    .mapInsideTransaction(SQLTransactionTask.transaction(action2))
            task shouldBe SQLTransactionTask.autoCommit(
                    Task.TaskLink(
                            action1,
                            action2))
        }

        "A transactional SQlTask should create a transaction from a task" {
            val transaction = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.transaction(transaction)
            task shouldBe SQLTransactionTask.transaction(transaction)
        }

        "An autocommit SQlTask should create a transaction from a task" {
            val transaction = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.autoCommit(transaction)
            task shouldBe SQLTransactionTask.autoCommit(transaction)
        }

        "A transactional SQLTransactionTask should create a transaction from a task and unwrap a SQLTransactionTask" {
            val transaction = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.transaction(SQLTransactionTask.transaction(transaction))
            val task2 = SQLTransactionTask.transaction(SQLTransactionTask.autoCommit(transaction))
            task shouldBe SQLTransactionTask.transaction(transaction)
            task2 shouldBe SQLTransactionTask.transaction(transaction)
        }

        "An autocommit SQLTransactionTask should create a transaction from a task and unwrap a SQLTransactionTask" {
            val transaction = Task.map<Int, String, SQLTaskContext<*>>({ it.toString() })
            val task = SQLTransactionTask.autoCommit(SQLTransactionTask.transaction(transaction))
            val task2 = SQLTransactionTask.autoCommit(SQLTransactionTask.autoCommit(transaction))
            task shouldBe SQLTransactionTask.autoCommit(transaction)
            task2 shouldBe SQLTransactionTask.autoCommit(transaction)
        }
    }
}
