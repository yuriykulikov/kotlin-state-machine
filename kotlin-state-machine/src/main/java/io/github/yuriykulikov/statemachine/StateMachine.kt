/*
 * MIT License
 *
 * Copyright (c) 2019 Yuriy Kulikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.yuriykulikov.statemachine

import kotlin.properties.Delegates

/**
 * # State Machine
 * State machine (SM) processes incoming events in its [currentState]. If this state cannot handle the event
 * ([State.onEvent] returned false), event is propagated to the parent of the state.
 *
 * ## State pattern
 * SM has a finite amount of states of which exactly one is the current state.
 * SM delegates processing of incoming events to this state.
 * State can be changed by the SM itself as a reaction to an external event.
 *
 * [State pattern on Wikipedia](https://en.wikipedia.org/wiki/State_pattern)
 *
 * [State Machine on Wikipedia](https://en.wikipedia.org/wiki/Finite-state_machine)
 *
 * ## Hierarchical Finite State Machine
 * States are organized as a rooted forest (union of rooted trees). SM has multiple sub-hierarchies, each one is a rooted tree.
 * If a state was not able to process an event (returned *false* from [State.onEvent]), the event if delivered
 * to the parent of this state.
 *
 * **Any state can be current state, not just leaf states**
 *
 * [Tree on Wikipedia](https://en.wikipedia.org/wiki/Tree_(graph_theory))
 *
 * ## Event propagation
 * Consider this SM, which is in the *x* state:
 * ```
 *          root       O
 *        /      \      \
 *       A        B      M
 *     /  \     /  \      \
 *   a    b   *x*    y     G
 * ```
 * If *x* is not able to handle an event, it is delivered to B, which is a parent of *x*. If B is also not able to
 * handle this event, it goes to the next parent. If no parent is available (root was not able to handle), an exception
 * is thrown. O-M-G is a separate hierarchy which is not used in this case (no common parent).
 *
 * [HFSM](https://en.wikipedia.org/wiki/UML_state_machine#Hierarchically_nested_states)
 *
 * ## Transitions
 * Transition is a change of the SM state due to an external event. Transitions trigger [State.exit] and [State.enter]
 * on the states which are involved in the transition. All states in the hierarchy are notified, not only the leaf states.
 *
 * Consider moving from *x* to *y*:
 * ```
 *
 *          root         ->             ->         root
 *        /      \       ->             ->       /      \
 *       A        B      -> from x to y ->      A        B
 *     /  \     /  \     ->             ->    /  \     /  \
 *   a    b   *x*    y   ->             ->   a    b  x    *y*
 * ```
 * States will be notified in this order: x.exit(), y.enter().
 * The B state will not get any event because root was not exited or entered.
 *
 *  * Consider moving from *a* to *y*:
 * ```
 *          root         ->             ->         root
 *        /      \       ->             ->       /      \
 *       A        B      -> from x to b ->      A        B
 *     /  \     /  \     ->             ->    /  \      /  \
 *  *a*    b   x    y    ->             ->   a    b    x   *y*
 * ```
 * States will be notified in this order: a.exit(), A.exit(), B.enter(), y.enter().
 * The root state will not get any event because root was not exited or entered.
 * A and B states are parent states of the leaf states and they also receive [State.enter] and [State.exit].
 * Both callbacks will receive the **reason** - an external event which has caused the transition.
 *
 * ## Transitions between trees
 *
 * Consider moving from *a* to *M*:
 * ```
 *          root        O       ->             ->         root        O
 *        /      \       \      ->             ->       /      \       \
 *       A        B       M     -> from x to L ->      A        B      *M*
 *     /  \     /  \       \    ->             ->    /  \     /  \       \
 *  *a*    b   x    y       G   ->             ->   a    b    x   y       G
 * ```
 * States will be notified in this order: a.exit(), A.exit(), root.exit(), O.enter(), M.enter().
 *
 * ## Origin
 * This particular state machine is a part of an [Simple Alarm Clock](https://github.com/yuriykulikov/AlarmClock)
 * open source project licensed under MIT license. It is inspired by the
 * [Android SM](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/com/android/internal/util/StateMachine.java)
 *
 * ## Example use
 *
 * ```
 * val stateMachine = StateMachine<Event>(
 *      name = "TestStateMachine",
 *      logger = object : SMLogger {
 *          override val isEnabled: Boolean = true
 *          override fun log(message: String) { println(message) }
 *      }
 *  )
 *
 * // start the state machine, add states using the configurator
 * stateMachine.start {
 *     addState(state = root)
 *     addState(state = s1, parent = root)
 *     addState(state = s2, parent = s1)
 *     // this state will be the initial state of the SM
 *     addState(state = s3, parent = s2, initial = true)
 * }
 * ```
 */
class StateMachine<T : Any>(
    val name: String,
    private val logger: SMLogger
) {
    /** State hierarchy as a tree. Root Node is null.*/
    private lateinit var tree: Map<State<T>, Node<T>>

    /** Current state of the state machine */
    private var currentState: State<T> by Delegates.notNull()

    /** State into which the state machine transitions */
    private var targetState: State<T> by Delegates.notNull()

    /**
     * Counter which is incremented when SM starts processing and decremented when it finishes.
     * Use to make sure that [transitionTo] is only used when SM is processing. See [withProcessingFlag].
     */
    private var processing: Int = 0

    /** Events not processed by current state and put on hold until the next transition */
    private val deferred = mutableListOf<T>()

    /**
     * Configure and start the state machine. [State.enter] will be called on the initial state and its parents.
     */
    fun start(event: T? = null, configurator: StateMachineBuilder.(StateMachineBuilder) -> Unit) {
        tree = StateMachineBuilder()
            .apply { configurator(this, this) }
            .mutableTree
            .toMap()
        enterInitialState(event)
    }

    /**
     * Process the event. If [State.onEvent] returns false, event goes to the next parent state.
     */
    fun sendEvent(event: T) = withProcessingFlag {
        val hierarchy = branchToRoot(currentState)

        logger.debug { "[$name] $event -> (${hierarchy.joinToString(" > ")})" }

        val processedIn: State<T>? = hierarchy.firstOrNull { state: State<T> ->
            logger.debug { "[$name] $state.processEvent()" }
            state.onEvent(event)
        }

        requireNotNull(processedIn) { "[$name] was not able to handle $event" }

        performTransitions(event)
    }

    /**
     * Trigger a transition. Allowed only while processing events. All exiting states will
     * receive [State.exit] and all entering states [State.enter] calls.
     * Transitions will be performed after the [sendEvent] is done.
     * Can be called from [State.enter]. In this case last call wins.
     */
    fun transitionTo(state: State<T>) {
        check(processing > 0) { "transitionTo can only be called within processEvent" }
        targetState = state
        logger.debug { "[$name] ${currentState.name} -> ${targetState.name}" }
    }

    /**
     * Indicate that current state defers processing of this event to the next state.
     * Event will be delivered upon state change to the next state.
     */
    fun deferEvent(event: T) {
        logger.debug { "[$name] deferring $event from $name to next state" }
        deferred.add(event)
    }

    /**
     * Loop until [currentState] is not the same as [targetState] which can be caused by [transitionTo]
     */
    private fun performTransitions(reason: T?) {
        check(processing > 0)
        while (currentState != targetState) {
            val currentBranch = branchToRoot(currentState)
            val targetBranch = branchToRoot(targetState)
            val common = currentBranch.intersect(targetBranch)

            val toExit = currentBranch.minus(common)
            val toEnter = targetBranch.minus(common).reversed()

            // now that we know the branches, change the current state to target state
            // calling exit/enter may change this afterwards
            currentState = targetState

            logger.debug { "[$name] exiting $toExit, entering $toEnter" }

            toExit.forEach { state -> state.exit(reason) }
            toEnter.forEach { state -> state.enter(reason) }

            processDeferred()
        }
    }

    private fun processDeferred() {
        val copy = deferred.toList()
        deferred.clear()
        copy.forEach { sendEvent(it) }
    }

    private fun branchToRoot(state: State<T>): List<State<T>> {
        return generateSequence(tree.getValue(state)) { it.parentNode }
            .map { it.state }
            .toList()
    }

    /** Goes all states from root to [currentState] and invokes [State.enter] */
    private fun enterInitialState(event: T?) = withProcessingFlag {
        val toEnter = branchToRoot(currentState).reversed()
        logger.debug { "[$name] entering $toEnter" }
        toEnter.forEach { it.enter(event) }
        performTransitions(event)
    }

    /**
     * Executes the [block] increasing [processing] before and decreasing it after the block execution.
     */
    private inline fun withProcessingFlag(block: () -> Unit) {
        processing++
        block()
        processing--
    }

    inner class StateMachineBuilder {
        internal val mutableTree = mutableMapOf<State<T>, Node<T>>()

        /**
         * Adds a new state. Parent must be added before it is used here as [parent].
         *
         * At least one state must have [initial] == true or [setInitialState]
         */
        fun addState(state: State<T>, parent: State<T>? = null, initial: Boolean = false) {
            val parentNode: Node<T>? = parent?.let {
                requireNotNull(mutableTree[it]) { "Parent $parent must be added before adding a child" }
            }

            mutableTree[state] = Node(state, parentNode)

            if (initial) setInitialState(state)
        }

        fun setInitialState(state: State<T>) {
            currentState = state
            targetState = state
        }
    }
}

/** Node in the state tree */
internal class Node<T>(
    var state: State<T>,
    var parentNode: Node<T>?
)

private inline fun SMLogger.debug(message: () -> String) {
    if (isEnabled) {
        log(message())
    }
}
