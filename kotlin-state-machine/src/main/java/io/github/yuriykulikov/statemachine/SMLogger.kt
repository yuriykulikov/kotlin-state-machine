package io.github.yuriykulikov.statemachine

/**
 * Logger interface for the [StateMachine].
 */
interface SMLogger {
    val isEnabled: Boolean
    fun log(message: String)
}