# Navigation

The back stack is Core state (`StackNavigator` in `ReflexApp.navigator`); Compose only draws it. ViewModels never
touch a navigator: they call their feature's port, and `ReflexApp` implements every port. `<pkg>` is
`androidshowcase.reflex`.

## Push a screen

1. Target route in its feature: `data class FooDetailRoute(val id: Int) : Route`, registered with
   `screen<FooDetailRoute> { route -> ... }`.
2. Port in the source feature's `<Name>Routes.kt`, named after what the user did, not the destination:

```kotlin
fun interface FooNavigation {
  fun openDetail(id: Int)
}
```

   Several exits make it a plain `interface` with one method each (`OnboardingNavigation`).
3. ViewModel: `state.next(task { navigation.openDetail(action.id); null })`.
4. `shell/.../ReflexApp.kt`:

```kotlin
private val fooNavigation = FooNavigation { id -> navigator.push(FooDetailRoute(id)) }

private fun shellModule() = module {
  // ...
  single<FooNavigation> { fooNavigation }
}
```

   `push` adds a screen; `replaceAll(...)` resets the stack when a flow ends (onboarding → Home). Cross-feature
   navigation is the same: feature A's port, implemented in the shell by pushing feature B's route.
5. `ReflexHost.ScreenContent` passes `onBack` to pushed screens that draw a back arrow. System back, predictive back
   and the `goBack` introspection action already come from the stack.
6. Behavior test: `act("open", """{"id":1}""", expect = "FooDetail")`, then `act("goBack", expect = "Foo")`.

## Add a tab

1. A `data object FooRoute : Route` with its screen registered as usual. The tab's introspection name is the route's
   `toString()` minus `Route`, so `selectTab {"tab":"Foo"}`.
2. `ReflexApp.shellModule()`: add it to `TabNavigator(listOf(PokedexRoute, FavoritesRoute, FooRoute), ::createScreen)`.
   Every tab's model lives as long as Home, so its state survives tab switches.
3. `app/.../ui/ReflexHost.kt` `HomeTabs`: add `is FooViewModel -> FooScreen(model)` to the tab content `when`. The
   icon and label currently branch on a `favorites` boolean; with a third tab, switch to `when (tab.route)`.
4. Label strings `tab_foo` in `app/src/main/res/values/strings.xml` and `values-vi/strings.xml`.
5. Behavior test: `act("selectTab", """{"tab":"Foo"}""", expect = "Foo")`, and `refused` for an unknown tab.

## Add a deep link

1. `shell/.../DeepLinks.kt` `parse`: a `when` branch returning the routes to push over Home, `null` for anything
   malformed.
2. `shell/src/test/kotlin/<pkg>/shell/DeepLinksTest.kt`: accepted forms (trailing slash, query) and rejected ones.
3. `app/src/main/AndroidManifest.xml`: `<data android:scheme="reflex" android:host="foo" />` in the deep-link
   intent filter (it lists only `pokemon` today).
4. Behavior test with the root's lent action: `act("openDeepLink", """{"uri":"reflex://foo/1"}""", ...)`, once on
   Home (the stack is rebuilt as Home + the link's routes) and once during onboarding (the link waits for Home).
