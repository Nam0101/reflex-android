package androidshowcase.reflex.domain.platform

import androidshowcase.reflex.domain.model.AppLanguage

/** Platform port: switch the UI language. Android uses per-app locales; the CLI records the call. */
fun interface LanguageApplier {
  fun apply(language: AppLanguage)
}
