package dev.yn.playground.user.models

sealed class UserError: Throwable() {
    data class NoSuchUser(val userName: String): UserError()
    object SessionExpired: UserError()
    data class NoSuchTrustedDevice(val userTrustedDevice: UserTrustedDevice): UserError()
    data class SessionAlreadyExists(val userId: String): UserError()
    object AuthenticationFailed: UserError()
    object AuthorizationFailed: UserError()
}