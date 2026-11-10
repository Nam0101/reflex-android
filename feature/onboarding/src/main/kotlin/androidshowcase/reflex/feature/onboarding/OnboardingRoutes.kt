package androidshowcase.reflex.feature.onboarding

import androidshowcase.reflex.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object LanguageRoute : Route

@Serializable
data object OnboardingRoute : Route

/** Where onboarding hands control back. The shell implements it: this feature never knows what comes next. */
interface OnboardingNavigation {
  fun languageChosen()

  fun onboardingFinished()
}
