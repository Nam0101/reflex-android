@file:OptIn(ExperimentalCoroutinesApi::class)

package androidshowcase.reflex.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReflexViewModelTest {
  private class CounterViewModel(scope: CoroutineScope, private val ticks: MutableSharedFlow<Int>) :
    ReflexViewModel<CounterViewModel.State, CounterViewModel.Action, CounterViewModel.Message>(State(), scope) {
    data class State(val count: Int = 0, val loaded: Int? = null, val starts: Int = 0)

    sealed interface Action {
      data object Increment : Action

      data object Load : Action

      data object StopTicks : Action

      data object Ignore : Action
    }

    sealed interface Message {
      data class ViewAction(val action: Action) : Message

      data class Loaded(val value: Int) : Message

      data class Tick(val value: Int) : Message

      data object Started : Message
    }

    override fun viewAction(action: Action) = Message.ViewAction(action)

    override fun onStart() = listOf(task { Message.Started }, stream<Message>("ticks", ticks.map { Message.Tick(it) }))

    override fun update(state: State, message: Message): Next<State, Message> = when (message) {
      is Message.ViewAction -> when (message.action) {
        Action.Increment -> state.copy(count = state.count + 1).next()
        Action.Load -> state.next(task { Message.Loaded(42) })
        Action.StopTicks -> state.next(Effect.Cancel("ticks"))
        Action.Ignore -> state.next()
      }

      is Message.Loaded -> state.copy(loaded = message.value).next()

      is Message.Tick -> state.copy(count = state.count + message.value).next()

      Message.Started -> state.copy(starts = state.starts + 1).next()
    }
  }

  @Test
  fun `actions go through update and effect results come back as messages`() = runTest {
    val vm = CounterViewModel(vmScope(), MutableSharedFlow())
    vm.send(CounterViewModel.Action.Increment)
    vm.send(CounterViewModel.Action.Load)
    advanceUntilIdle()
    assertEquals(CounterViewModel.State(count = 1, loaded = 42), vm.state.value)
  }

  @Test
  fun `start runs its effects once and streams stop when cancelled or closed`() = runTest {
    val ticks = MutableSharedFlow<Int>()
    val vm = CounterViewModel(vmScope(), ticks)
    vm.start()
    vm.start()
    advanceUntilIdle()
    ticks.emit(10)
    advanceUntilIdle()
    assertEquals(1, vm.state.value.starts)
    assertEquals(10, vm.state.value.count)

    vm.send(CounterViewModel.Action.StopTicks)
    advanceUntilIdle()
    ticks.emit(5)
    advanceUntilIdle()
    assertEquals(10, vm.state.value.count)

    vm.close()
    vm.send(CounterViewModel.Action.Load)
    advanceUntilIdle()
    assertEquals(null, vm.state.value.loaded)
  }

  @Test
  fun `send tells whether update did anything, and the scope's activity is busy until effects end`() = runTest {
    val activity = EffectActivity()
    val ticks = MutableSharedFlow<Int>()
    val vm = CounterViewModel(CoroutineScope(StandardTestDispatcher(testScheduler) + activity), ticks)
    assertEquals(false, vm.send(CounterViewModel.Action.Ignore))
    assertEquals(true, vm.send(CounterViewModel.Action.Increment))
    assertEquals(true, activity.isIdle)

    assertEquals(true, vm.send(CounterViewModel.Action.Load)) // same state, but a task runs
    assertEquals(false, activity.isIdle)
    advanceUntilIdle()
    assertEquals(true, activity.isIdle)

    vm.start() // one task, and a stream that is busy until its first value
    advanceUntilIdle()
    assertEquals(false, activity.isIdle)
    ticks.emit(1)
    advanceUntilIdle()
    assertEquals(true, activity.isIdle)
  }
}

/** Foreground work on the test scheduler (advanceUntilIdle skips backgroundScope), not a child of the test job. */
private fun TestScope.vmScope() = CoroutineScope(StandardTestDispatcher(testScheduler))
