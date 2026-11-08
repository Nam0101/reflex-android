package androidshowcase.reflex.domain.usecase

import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.domain.model.UserSettings
import androidshowcase.reflex.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** Business rule for the first screen: language first, then onboarding, then home. */
class ResolveLaunchStep(private val settings: SettingsRepository) {
  suspend operator fun invoke(): LaunchStep = resolve(settings.settings.first())

  companion object {
    fun resolve(settings: UserSettings): LaunchStep = when {
      settings.language == null -> LaunchStep.ChooseLanguage
      !settings.onboardingCompleted -> LaunchStep.Onboarding
      else -> LaunchStep.Home
    }
  }
}
