package androidshowcase.reflex.domain.usecase

import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.domain.model.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolveLaunchStepTest {
  @Test
  fun `no language chosen asks for language even if onboarding was completed`() {
    assertEquals(LaunchStep.ChooseLanguage, ResolveLaunchStep.resolve(UserSettings(null, onboardingCompleted = true)))
  }

  @Test
  fun `language chosen but onboarding pending shows onboarding`() {
    assertEquals(LaunchStep.Onboarding, ResolveLaunchStep.resolve(UserSettings(AppLanguage.English, false)))
  }

  @Test
  fun `both done lands on home`() {
    assertEquals(LaunchStep.Home, ResolveLaunchStep.resolve(UserSettings(AppLanguage.Vietnamese, true)))
  }
}
