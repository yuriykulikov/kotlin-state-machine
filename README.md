<a href='https://github.com/yuriykulikov/kotlin-state-machine/actions?query=workflow%3A%22CI%22'><img src='https://github.com/yuriykulikov/kotlin-state-machine/workflows/CI/badge.svg'></a>
[![codecov.io](http://codecov.io/github/yuriykulikov/kotlin-state-machine/coverage.svg?branch=main)](http://codecov.io/github/yuriykulikov/kotlin-state-machine?branch=main)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.yuriykulikov/kotlin-state-machine/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.yuriykulikov/kotlin-state-machine)

# kotlin-state-machine
Hierarchical state machine written in Kotlin

## State Machine
State machine (SM) processes incoming events in its [currentState]. If this state cannot handle the event
([State.onEvent] returned false), event is propagated to the parent of the state.

## State pattern
SM has a finite amount of states of which exactly one is the current state.
SM delegates processing of incoming events to this state.
State can be changed by the SM itself as a reaction to an external event.

[State pattern on Wikipedia](https://en.wikipedia.org/wiki/State_pattern)

[State Machine on Wikipedia](https://en.wikipedia.org/wiki/Finite-state_machine)

## Hierarchical Finite State Machine
States are organized as a rooted forest (union of rooted trees). SM has multiple sub-hierarchies, each one is a rooted tree.
If a state was not able to process an event (returned *false* from [State.onEvent]), the event if delivered
to the parent of this state.

**Any state can be current state, not just leaf states**

[Tree on Wikipedia](https://en.wikipedia.org/wiki/Tree_(graph_theory))

## Event propagation
Consider this SM, which is in the *x* state:
```
         root       O
       /      \      \
      A        B      M
    /  \     /  \      \
  a    b   *x*    y     G
```
If *x* is not able to handle an event, it is delivered to B, which is a parent of *x*. If B is also not able to
handle this event, it goes to the next parent. If no parent is available (root was not able to handle), an exception
is thrown. O-M-G is a separate hierarchy which is not used in this case (no common parent).

[HFSM](https://en.wikipedia.org/wiki/UML_state_machine#Hierarchically_nested_states)

## Transitions
Transition is a change of the SM state due to an external event. Transitions trigger [State.exit] and [State.enter]
on the states which are involved in the transition. All states in the hierarchy are notified, not only the leaf states.

Consider moving from *x* to *y*:
```

         root         ->             ->         root
       /      \       ->             ->       /      \
      A        B      -> from x to y ->      A        B
    /  \     /  \     ->             ->    /  \     /  \
  a    b   *x*    y   ->             ->   a    b  x    *y*
```
States will be notified in this order: x.exit(), y.enter().
The B state will not get any event because root was not exited or entered.

 * Consider moving from *a* to *y*:
```
         root         ->             ->         root
       /      \       ->             ->       /      \
      A        B      -> from x to b ->      A        B
    /  \     /  \     ->             ->    /  \      /  \
 *a*    b   x    y    ->             ->   a    b    x   *y*
```
States will be notified in this order: a.exit(), A.exit(), B.enter(), y.enter().
The root state will not get any event because root was not exited or entered.
A and B states are parent states of the leaf states and they also receive [State.enter] and [State.exit].
Both callbacks will receive the **reason** - an external event which has caused the transition.

## Transitions between trees

Consider moving from *a* to *M*:
```
         root        O       ->             ->         root        O
       /      \       \      ->             ->       /      \       \
      A        B       M     -> from x to L ->      A        B      *M*
    /  \     /  \       \    ->             ->    /  \     /  \       \
 *a*    b   x    y       G   ->             ->   a    b    x   y       G
```
States will be notified in this order: a.exit(), A.exit(), root.exit(), O.enter(), M.enter().

## Origin
This particular state machine is a part of an open source project licensed under MIT license.
It is inspired by an
[Android SM](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/com/android/internal/util/StateMachine.java)
