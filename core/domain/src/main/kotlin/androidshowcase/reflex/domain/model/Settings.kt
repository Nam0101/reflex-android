package androidshowcase.reflex.domain.model

import kotlinx.serialization.Serializable

/** Languages the app ships resources for. [tag] is a BCP 47 language tag. */
@Serializable
enum class AppLanguage(val tag: String, val nativeName: String) {
  English("en", "English"),
  Vietnamese("vi", "Tiếng Việt"),
  ;

  companion object {
    fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
  }
}

/** [language] is null until the user picks one on first launch. */
data class UserSettings(val language: AppLanguage?, val onboardingCompleted: Boolean)

/** Where a launch lands, decided by [androidshowcase.reflex.domain.usecase.ResolveLaunchStep]. */
enum class LaunchStep { ChooseLanguage, Onboarding, Home }
