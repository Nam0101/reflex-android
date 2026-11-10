package androidshowcase.reflex.shell

import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.domain.usecase.ResolveLaunchStep
import androidshowcase.reflex.introspection.ViewModelIntrospectionNode
import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.task
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute : Route

/** Decides the first screen. While it is on top, Android keeps the system splash screen up. */
class SplashViewModel(
  private val resolveLaunchStep: ResolveLaunchStep,
  private val onResolved: (LaunchStep) -> Unit,
  scope: CoroutineScope,
) : ReflexViewModel<SplashViewModel.State, Nothing, SplashViewModel.Message>(State(), scope) {
  @Serializable
  data class State(val step: LaunchStep? = null)

  sealed interface Message {
    data class Resolved(val step: LaunchStep) : Message
  }

  override fun viewAction(action: Nothing): Message = action

  override fun onStart() = listOf(task<Message> { Message.Resolved(resolveLaunchStep()) })

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.Resolved -> state.copy(step = message.step).next(
      task {
        onResolved(message.step)
        null
      },
    )
  }
}

class SplashViewModelIntrospectionNode(viewModel: SplashViewModel) :
  ViewModelIntrospectionNode<SplashViewModel.State, Nothing>(viewModel, SplashViewModel.State.serializer()) {
  override val name = "Splash"
  override val actions = emptyList<androidshowcase.reflex.introspection.IntrospectionAction>()
}
