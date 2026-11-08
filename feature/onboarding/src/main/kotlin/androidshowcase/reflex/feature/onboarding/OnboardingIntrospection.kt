package androidshowcase.reflex.feature.onboarding

import androidshowcase.reflex.introspection.ViewModelIntrospectionNode
import androidshowcase.reflex.introspection.action

class LanguageViewModelIntrospectionNode(viewModel: LanguageViewModel) :
  ViewModelIntrospectionNode<LanguageViewModel.State, LanguageViewModel.Action>(
    viewModel,
    LanguageViewModel.State.serializer(),
  ) {
  override val name = "Language"

  override val actions = listOf(
    action("select", "Select a language", LanguageViewModel.Action.Select.serializer()) { send(it) },
    action("continue", "Continue") { send(LanguageViewModel.Action.Continue) },
  )
}

class OnboardingViewModelIntrospectionNode(viewModel: OnboardingViewModel) :
  ViewModelIntrospectionNode<OnboardingViewModel.State, OnboardingViewModel.Action>(
    viewModel,
    OnboardingViewModel.State.serializer(),
  ) {
  override val name = "Onboarding"

  override val actions = listOf(
    action("next", "Next page") { send(OnboardingViewModel.Action.Next) },
    action("showPage", "Swipe to a page", OnboardingViewModel.Action.ShowPage.serializer()) { send(it) },
    action("finish", "Get started / Skip") { send(OnboardingViewModel.Action.Finish) },
  )
}
