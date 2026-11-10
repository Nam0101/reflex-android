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
  private val activity = parentScope.coroutineContext[EffectActivity]
  private val started = AtomicBoolean(false)

  val state: StateFlow<State> = mutableState.asStateFlow()

  /** Returns false when [update] ignored the action: it returned the same state and no effect. */
  fun send(action: Action): Boolean = dispatch(viewAction(action))

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

  private fun dispatch(message: Message): Boolean {
    var effects: List<Effect<Message>> = emptyList()
    var changed = false
    mutableState.update { current ->
      val next = update(current, message)
      effects = next.effects
      changed = next.state != current
      next.state
    }
    effects.forEach(::run)
    return changed || effects.isNotEmpty()
  }

  private fun run(effect: Effect<Message>) {
    when (effect) {
      is Effect.Task -> {
        val done = activity?.begin()
        scope.launch { effect.block()?.let(::dispatch) }.invokeOnCompletion { done?.invoke() }
      }

      is Effect.Stream -> {
        // A stream counts as busy until its first value: the screen is still loading until then.
        val done = activity?.begin()
        val job = scope.launch(start = CoroutineStart.LAZY) {
          effect.flow.collect {
            dispatch(it)
            done?.invoke()
          }
        }
        streams.put(effect.key, job)?.cancel()
        // A stream that ended is forgotten, unless a newer one already took its key.
        job.invokeOnCompletion {
          streams.remove(effect.key, job)
          done?.invoke()
        }
        job.start()
      }

      is Effect.Cancel -> streams.remove(effect.key)?.cancel()
    }
  }
}
