package androidshowcase.reflex.viewmodel

import kotlinx.coroutines.flow.Flow

/** What [ReflexViewModel.update] returns: the new state plus side effects to run after it is published. */
class Next<out State, out Message>(val state: State, val effects: List<Effect<Message>> = emptyList())

/** `state.copy(isLoading = true).next(load())`, or `state.next()` for no effect. */
fun <State, Message> State.next(vararg effects: Effect<Message>): Next<State, Message> = Next(this, effects.asList())

/** A side effect. Its results come back into `update` as messages, never as direct state writes. */
sealed interface Effect<out Message> {
  /** One-shot work (a service call, navigation). A non-null result is dispatched as a message. */
  class Task<out Message>(val block: suspend () -> Message?) : Effect<Message>

  /** A subscription. Starting a stream with a [key] that is already running replaces the old one. */
  class Stream<out Message>(val key: Any, val flow: Flow<Message>) : Effect<Message>

  class Cancel(val key: Any) : Effect<Nothing>
}

fun <Message> task(block: suspend () -> Message?): Effect<Message> = Effect.Task(block)

fun <Message> stream(key: Any, flow: Flow<Message>): Effect<Message> = Effect.Stream(key, flow)
