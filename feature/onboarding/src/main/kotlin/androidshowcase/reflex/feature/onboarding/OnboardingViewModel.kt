package androidshowcase.reflex.feature.onboarding

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.repository.SettingsRepository
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.task
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

class OnboardingViewModel(
  private val settings: SettingsRepository,
  private val navigation: OnboardingNavigation,
  scope: CoroutineScope,
) : ReflexViewModel<OnboardingViewModel.State, OnboardingViewModel.Action, OnboardingViewModel.Message>(
  State(),
  scope,
) {
  @Serializable
  data class State(
    val page: Int = 0,
    val pageCount: Int = PAGE_COUNT,
    val isSaving: Boolean = false,
    val error: AppError? = null,
  ) {
    val isLastPage = page == pageCount - 1
  }

  sealed interface Action {
    data object Next : Action

    /** The user swiped to [page]. */
    @Serializable
    data class ShowPage(val page: Int) : Action

    /** "Get started" on the last page, "Skip" on the others. */
    data object Finish : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class Saved(val result: Result<Unit, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (val action = message.action) {
      Action.Next -> state.copy(page = (state.page + 1).coerceAtMost(state.pageCount - 1)).next()

      is Action.ShowPage -> state.copy(page = action.page.coerceIn(0, state.pageCount - 1)).next()

      Action.Finish ->
        if (state.isSaving) {
          state.next()
        } else {
          state.copy(isSaving = true, error = null).next(task { Message.Saved(settings.completeOnboarding()) })
        }
    }

    is Message.Saved -> message.result.fold(
      success = {
        state.copy(isSaving = false).next(
          task {
            navigation.onboardingFinished()
            null
          },
        )
      },
      failure = { state.copy(isSaving = false, error = it).next() },
    )
  }

  private companion object {
    const val PAGE_COUNT = 3
  }
}
