package io.github.yuriykulikov.statemachine

/**
 * Event handler in a [StateMachine]
 */
abstract class State<T> {
    /**
     * State is entered. It is not called if the state is re-entered (transition to self).
     *
     * [reason] is the message which caused and and *null* if [enter] is called from the initial state is entered.
     */
    abstract fun enter(reason: T?)

    /**
     * State is exited. It is only called if the state is completely left. Transitions in the child states do not
     * cause this.
     *
     * [reason] is the message which caused and and *null* if [exit] is called from the initial state is entered.
     */
    abstract fun exit(reason: T?)

    /**
     * Process events in the state. Return *true* if the state has handled the event and *false* if not.
     * Unhandled event will be propagated to the parent.
     */
    abstract fun onEvent(event: T): Boolean

    open val name: String = javaClass.simpleName

    override fun toString(): String = name
}