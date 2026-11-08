package androidshowcase.reflex.feature.onboarding

import androidshowcase.reflex.navigation.UiScope
import androidshowcase.reflex.navigation.screen
import org.koin.core.module.Module
import org.koin.dsl.module

/** The onboarding screens. The shell loads this module and implements [OnboardingNavigation]. */
fun onboardingModule(): Module = module {
  screen<LanguageRoute> { LanguageViewModel(get(), get(), get(), get(UiScope)) }
  screen<OnboardingRoute> { OnboardingViewModel(get(), get(), get(UiScope)) }
}
