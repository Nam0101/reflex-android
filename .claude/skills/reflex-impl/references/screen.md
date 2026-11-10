# A screen

`Foo` stands for the screen, `<name>` for its feature module, `<pkg>` for `androidshowcase.reflex`. Closest real
examples: `FavoritesViewModel` (stream + actions with input), `PokedexViewModel` (paging, stale guard),
`PokemonDetailViewModel` (route argument, two independent loads), `SplashViewModel` (no user actions).

Contents: Route · ViewModel · Variants · Introspection · Registration · View · Strings · ViewModel unit test ·
Behavior test

## Route

`feature/<name>/src/main/kotlin/<pkg>/feature/<name>/<Name>Routes.kt`, next to the feature's navigation port:

```kotlin
data object FooRoute : Route

/** Arguments travel in the route; Koin hands the route to the ViewModel. */
data class FooDetailRoute(val id: Int) : Route
```

A tab route must be a `data object`: its tab name comes from `toString()`.

## ViewModel

`feature/<name>/src/main/kotlin/<pkg>/feature/<name>/FooViewModel.kt`:

```kotlin
package androidshowcase.reflex.feature.foo

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.FooItem
import androidshowcase.reflex.domain.repository.FooRepository
import androidshowcase.reflex.introspection.ActionTitle
import androidshowcase.reflex.introspection.Introspected
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.task
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Introspected
class FooViewModel(
  private val repository: FooRepository,
  private val navigation: FooNavigation,
  scope: CoroutineScope,
) : ReflexViewModel<FooViewModel.State, FooViewModel.Action, FooViewModel.Message>(State(), scope) {
  @Serializable
  data class State(
    val items: List<FooItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
  ) {
    val isEmpty = !isLoading && error == null && items.isEmpty()
  }

  sealed interface Action {
    data object Retry : Action

    @ActionTitle("Open an item")
    @Serializable
    data class Open(val id: Int) : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class Loaded(val result: Result<List<FooItem>, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun onStart() = listOf(load())

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (val action = message.action) {
      Action.Retry -> if (state.isLoading) state.next() else state.copy(isLoading = true, error = null).next(load())

      is Action.Open -> state.next(
        task {
          navigation.openDetail(action.id)
          null
        },
      )
    }

    is Message.Loaded -> message.result.fold(
      success = { state.copy(items = it, isLoading = false, error = null).next() },
      failure = { state.copy(isLoading = false, error = it).next() },
    )
  }

  private fun load() = task<Message> { Message.Loaded(repository.items()) }
}
```

`when` branches have a blank line between them (ktlint layout used across the repo); `spotlessApply` fixes the rest.

## Variants

- Route argument: take it first in the constructor and seed state, `State(id = id)` (`PokemonDetailViewModel`).
- Observed data: in `onStart`, `stream<Message>(ITEMS_STREAM, repository.observe().map(Message::Changed))` with
  `private companion object { const val ITEMS_STREAM = "items" }`. Stop one with `state.next(Effect.Cancel(KEY))`.
- Writes: `state.copy(isSaving = true, error = null).next(task { Message.Saved(repository.save(x)) })`, then clear
  `isSaving` in both `fold` branches.
- Two failure sources on one screen get two fields (`error` for the load, `favoriteError` for the write).
- No user actions: `ReflexViewModel<State, Nothing, Message>` and `override fun viewAction(action: Nothing): Message =
  action`.

## Introspection

`@Introspected` on the ViewModel is all of it. At build time KSP (`core:introspection-processor`) writes
`FooViewModelIntrospectionNode` and registers it for the shell; there is no node file and no `resolveNode` branch.

- Screen name: the class name minus `ViewModel` (`Foo`); `@Introspected(name = "…")` overrides it.
- One action per subclass of the sealed `Action`, in declaration order, named after it: `Retry` → `retry`,
  `ToggleFavorite` → `toggleFavorite`. So name actions as verbs, one per button or gesture.
- Title (what `ls` shows): the name's words (`Toggle favorite`), or `@ActionTitle("…")` on the subclass.
- A `data object` takes no input; a class is the input and must be `@Serializable`. The build fails with the
  reason when `Action` is not sealed, an input class is not serializable, or the ViewModel does not extend
  `ReflexViewModel` directly.

Availability rules stay in `update`: an action sent at the wrong time returns `state.next()` there, and the driver
sees it as ignored.

## Registration

1. Koin, the feature's `<Name>Module.kt`:
   ```kotlin
   screen<FooRoute> { FooViewModel(get(), get(), get(UiScope)) }
   screen<FooDetailRoute> { route -> FooDetailViewModel(route.id, get(), get(UiScope)) }
   ```
   `get(UiScope)` is always the scope argument. A use case is built inline: `FooViewModel(LoadFoo(get()), ...)`.
2. `app/src/main/kotlin/<pkg>/ui/ReflexHost.kt`: `is FooViewModel -> FooScreen(model)` in `ScreenContent` (pushed
   screens with a back arrow take `onBack`: `FooDetailScreen(model, onBack)`), or in `HomeTabs` for a tab.
3. If the screen navigates, the shell implements its port: see navigation.md.

`WiringTest` (`:cli:test`) fails when either registration is missing.

## View

`feature/<name>-ui/src/main/kotlin/<pkg>/feature/<name>/ui/FooScreen.kt`:

```kotlin
@Composable
fun FooScreen(viewModel: FooViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  FooContent(state, viewModel::send)
}

@Composable
fun FooContent(state: State, onAction: (Action) -> Unit) {
  val error = state.error
  when {
    state.isLoading && state.items.isEmpty() -> LoadingState()

    error != null && state.items.isEmpty() -> ErrorState(error, onRetry = { onAction(Action.Retry) })

    else -> LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(contentType = "header") { LargeHeader(stringResource(R.string.foo_title), subtitle = null) }
      items(state.items, key = { it.id }, contentType = { "foo" }) { item ->
        FooRow(item, onClick = { onAction(Action.Open(item.id)) }, Modifier.animateItem())
      }
    }
  }
}

@Preview
@Composable
private fun FooPreview() = ReflexTheme {
  FooContent(State(items = listOf(FooItem(1, "Sample")), isLoading = false), onAction = {})
}
```

- Import `FooViewModel.Action` and `FooViewModel.State` directly (as `FavoritesScreen` does). `R` is the `-ui`
  module's own, same package, no import.
- Shared pieces live in `core:designsystem`: `ReflexTheme`, `LoadingState`, `ErrorState`, `errorMessage`,
  `LargeHeader`, `PokeballMark`. Add a component there only when two features need it.
- Lazy lists always pass `key` and `contentType`. Expensive per-item work (image palette) is cached off the main
  thread (`rememberArtworkColor`).
- Edge-to-edge is on: a full-screen pushed view handles `statusBarsPadding` / `navigationBarsPadding` itself; tab
  content gets the Scaffold padding from `HomeTabs`.

## Strings

`feature/<name>-ui/src/main/res/values/strings.xml` and `values-vi/strings.xml`, same names, prefixed by screen:

```xml
<string name="foo_title">Foo</string>
<plurals name="foo_count">
    <item quantity="one">%1$d item</item>
    <item quantity="other">%1$d items</item>
</plurals>
```

Vietnamese plurals need only `quantity="other"`. Write the Vietnamese with full diacritics.

## ViewModel unit test

`feature/<name>/src/test/kotlin/<pkg>/feature/<name>/FooViewModelTest.kt`. JUnit 6 and coroutines-test come from
the `reflex.jvm.library` convention plugin; no Gradle edit needed. Fakes implement the domain port by hand.

```kotlin
@file:OptIn(ExperimentalCoroutinesApi::class)

package androidshowcase.reflex.feature.foo

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.FooItem
import androidshowcase.reflex.domain.repository.FooRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class FooViewModelTest {
  private class FakeFooRepository(var result: Result<List<FooItem>, AppError>) : FooRepository {
    var calls = 0

    override suspend fun items(): Result<List<FooItem>, AppError> = result.also { calls++ }
  }

  @Test
  fun `a failed load shows the error, retry loads once and recovers`() = runTest {
    val repository = FakeFooRepository(Err(AppError.Network()))
    val opened = mutableListOf<Int>()
    val vm = FooViewModel(repository, FooNavigation { opened += it }, vmScope())
    vm.start()
    advanceUntilIdle()
    assertInstanceOf(AppError.Network::class.java, vm.state.value.error)

    repository.result = Ok(listOf(FooItem(1, "a")))
    vm.send(FooViewModel.Action.Retry)
    vm.send(FooViewModel.Action.Retry) // ignored while loading
    advanceUntilIdle()
    assertEquals(2, repository.calls)
    assertEquals(listOf(FooItem(1, "a")), vm.state.value.items)

    vm.send(FooViewModel.Action.Open(1))
    advanceUntilIdle()
    assertEquals(listOf(1), opened)
  }
}

/** Not a child of the test job: a never-ending stream would otherwise keep runTest waiting. */
private fun TestScope.vmScope() = CoroutineScope(StandardTestDispatcher(testScheduler))
```

Call `vm.start()` yourself: the navigator does it in the app. Test `update` branches that are easy to get wrong:
stale results, repeated submits, error then retry.

## Behavior test

`cli/src/test/kotlin/<pkg>/cli/behavior/FooBehaviorTest.kt`, one class per feature, one test per user journey,
named as a sentence:

```kotlin
class FooBehaviorTest : BehaviorTest() {
  @Test
  fun `open Foo, see its items, a failure shows the error and retry recovers`() {
    session.fixtures.failOnce("foo", HttpStatusCode.InternalServerError) // the first GET …/foo fails
    onboard() // first launch up to the loaded Pokédex
    app.act(
      "selectTab",
      """{"tab":"Foo"}""",
      expect = "Foo",
      state = """{"isLoading":false,"error":{"type":"Server"}}""",
    )

    app.act("retry", state = """{"error":null,"items":[{"id":1},{"id":2}]}""")
    app.refused("open", """{"id":"one"}""")
    app.act("open", """{"id":1}""", expect = "FooDetail", state = """{"id":1,"isLoading":false}""")
    app.act("goBack", expect = "Foo")
  }
}
```

- `precondition(screen, state)` waits for a screen; `act(action, input, expect, state)` acts then waits (`expect`
  defaults to the current screen) and fails when the app ignored the action; `ignored(action, input)` asserts the
  app accepted it and did nothing (`update` returned `state.next()`); `refused(action, input)` asserts the app
  rejects it (unavailable action, bad input).
- State is a JSON subset: objects match on the keys given; arrays must match in size, each element as a subset
  (`"items":[]` asserts empty, `[{},{}]` asserts two).
- `session.fixtures.failOnce(name, status)` fails the next request for a fixture, `delay(name, duration)` slows
  every one (to observe a loading state); declare them before the journey. `session.relaunch()` restarts on the
  same disk (proves persistence); `session.clock.advanceDays(n)` moves the app's clock;
  `session.appliedLanguages` shows what reached a platform port.
- Every HTTP request needs a fixture (data.md). Polling is built in: no sleeps.
- Cover the happy path, one refusal, one ignored action, one failure with recovery, and a relaunch when the feature
  writes to disk.
