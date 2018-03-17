package dev.yn.playground.user

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.common.ApplicationContextProvider
import dev.yn.playground.consumer.alpha.ConsumerExecutor
import dev.yn.playground.consumer.alpha.ConsumerExecutorProvider

val userCreatedAddress = "user.updated"
val userLoginAddress = "user.login"

fun userLoginConsumer(consumerExecutor: ConsumerExecutorProvider, applicationContextProvider: ApplicationContextProvider): ConsumerExecutor<ApplicationContext> {
    return consumerExecutor.buildExecutor(userLoginAddress, applicationContextProvider)
}

fun userCreateConsumer(consumerExecutor: ConsumerExecutorProvider, applicationContextProvider: ApplicationContextProvider): ConsumerExecutor<ApplicationContext> {
    return consumerExecutor.buildExecutor(userCreatedAddress, applicationContextProvider)
}