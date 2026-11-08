package androidshowcase.reflex.viewmodel

import androidshowcase.reflex.navigation.ScreenModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Every screen ViewModel has this shape (Elm-style):
 *
 * - [State]: everything the screen shows. Views and the CLI read it, nobody writes it from outside.
 * - [Action]: the public surface, dispatched with [send]. Views and the CLI use the same actions.
 * - [Message]: the internal surface. Actions arrive wrapped by [viewAction]; effect results arrive as
 *   their own messages.
 * - [update]: the only place state changes. Every path converges there.
 * - Services (network, storage, platform) come in through the constructor as interfaces.
 *
 * The owner (a navigator) calls [start] once after construction and [close] when the screen leaves.
 */
abstract class ReflexViewModel<State : Any, Action : Any, Message : Any>(
  initialState: State,
  parentScope: CoroutineScope,
) : ScreenModel {
  private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
  private val mutableState = MutableStateFlow(initialState)
  private val streams = ConcurrentHashMap<Any, Job>()
  private val started = AtomicBoolean(false)

  val state: StateFlow<State> = mutableState.asStateFlow()

  fun send(action: Action) = dispatch(viewAction(action))

  /** Wraps a public action into the internal message type, by convention `Message.ViewAction(action)`. */
  protected abstract fun viewAction(action: Action): Message

  /** Effects to run once when the screen is created: initial loads, subscriptions. */
  protected open fun onStart(): List<Effect<Message>> = emptyList()

  /**
   * Returns the next state and the effects to run. Must be pure and fast: no I/O, no navigation,
   * no service calls (put those in effects). Under contention it can run more than once per message.
   */
  protected abstract fun update(state: State, message: Message): Next<State, Message>

  final override fun start() {
    if (started.compareAndSet(false, true)) onStart().forEach(::run)
  }

  override fun close() = scope.cancel()

  private fun dispatch(message: Message) {
    var effects: List<Effect<Message>> = emptyList()
    mutableState.update { current -> update(current, message).also { effects = it.effects }.state }
    effects.forEach(::run)
  }

  private fun run(effect: Effect<Message>) {
    when (effect) {
      is Effect.Task -> scope.launch { effect.block()?.let(::dispatch) }

      is Effect.Stream -> {
        val job = scope.launch(start = CoroutineStart.LAZY) { effect.flow.collect(::dispatch) }
        streams.put(effect.key, job)?.cancel()
        job.start()
      }

      is Effect.Cancel -> streams.remove(effect.key)?.cancel()
    }
  }
}
