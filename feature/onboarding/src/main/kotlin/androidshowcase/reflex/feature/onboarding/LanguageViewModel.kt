package androidshowcase.reflex.feature.onboarding

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.domain.platform.LanguageApplier
import androidshowcase.reflex.domain.repository.SettingsRepository
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.task
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

class LanguageViewModel(
  private val settings: SettingsRepository,
  private val languageApplier: LanguageApplier,
  private val navigation: OnboardingNavigation,
  scope: CoroutineScope,
) : ReflexViewModel<LanguageViewModel.State, LanguageViewModel.Action, LanguageViewModel.Message>(State(), scope) {
  @Serializable
  data class State(
    val languages: List<AppLanguage> = AppLanguage.entries,
    val selected: AppLanguage? = null,
    val isSaving: Boolean = false,
    val error: AppError? = null,
  ) {
    val canContinue = selected != null && !isSaving
  }

  sealed interface Action {
    @Serializable
    data class Select(val language: AppLanguage) : Action

    data object Continue : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class Loaded(val current: AppLanguage?) : Message

    data class Saved(val language: AppLanguage, val result: Result<Unit, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun onStart() = listOf(task<Message> { Message.Loaded(settings.settings.first().language) })

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (val action = message.action) {
      is Action.Select -> state.copy(selected = action.language, error = null).next()

      Action.Continue -> {
        val language = state.selected
        if (language == null || state.isSaving) {
          state.next()
        } else {
          state.copy(isSaving = true, error = null).next(
            task {
              Message.Saved(language, settings.setLanguage(language))
            },
          )
        }
      }
    }

    // A choice the user already made on this screen wins over the persisted one.
    is Message.Loaded -> state.copy(selected = state.selected ?: message.current).next()

    is Message.Saved -> message.result.fold(
      success = {
        state.copy(isSaving = false).next(
          task {
            languageApplier.apply(message.language)
            navigation.languageChosen()
            null
          },
        )
      },
      failure = { state.copy(isSaving = false, error = it).next() },
    )
  }
}
