# AGENTS.md

Multi-module Android base project built on Shopify's Reflex architecture (https://shopify.engineering/reflex) and
Clean Architecture. The app's whole behavior lives in a pure-JVM **Core**; the Compose **Mobile** layer only
renders Core state and sends actions; the **CLI** drives the same Core headlessly or over a socket to a debug build.
README.md has the module map and commands.

## Layer rules

- Core modules (`core:domain`, `core:data`, `core:viewmodel`, `core:introspection`, `feature:<name>`, `shell`) are
  Kotlin/JVM modules: `android.*` does not compile there. Put platform needs behind a port in `core:domain`
  (see `LanguageApplier`) and implement it in `:app` (`ReflexApplication.androidPlatform`).
- Navigation is Core state. `StackNavigator` / `TabNavigator` own the screens; Compose only draws them with
  Navigation 3. A ViewModel navigates by calling its feature's navigation interface (`PokedexNavigation`) from an
  effect; `ReflexApp` implements those interfaces.
- `update(state, message)` is pure: it returns the next state plus effects (`task`, `stream`, `Effect.Cancel`).
  Every suspend call and side effect runs inside an effect and reports back as a `Message`.
- Errors are values: repositories return `Result<T, AppError>`; `catchingRemote` / `catchingLocal` rethrow
  `CancellationException`. State exposes `AppError?`; views map it to text with `errorMessage`.
- Dependencies are constructor parameters, wired with the Koin DSL: data and platform ports in
  `shell/DataModule.kt`, screens in each feature's module (`pokedexModule`) with `screen<Route> { }`. ViewModels
  never call Koin, so tests construct them by hand. Each `ReflexApp` owns a `koinApplication`; never `startKoin`.

## Adding a screen

Each step is done when its check holds.

1. **Route**: a `data object` / `data class` implementing `Route` in the feature's Core module. Check: it compiles
   without any Compose import.
2. **ViewModel**: extend `ReflexViewModel<State, Action, Message>`. `State` is an immutable `@Serializable` data
   class holding everything the view shows; `Action` is what a user can do (`@Serializable` when it carries input);
   `Message` wraps `ViewAction(action)` plus effect results. Check: a unit test drives `send` and asserts `state`.
3. **Introspection node**: extend `ViewModelIntrospectionNode`, one `action(...)` per user action, named as a
   camelCase verb (`toggleFavorite`). Check: every button or gesture in the view has a matching node action.
4. **Register** the screen in three places: `screen<YourRoute> { }` in the feature's Koin module (a new feature also
   adds its module to `modules(...)` in `ReflexApp`), `resolveNode` in `shell/AppIntrospection.kt`, and
   `ScreenContent` (or `HomeTabs`) in `app/.../ui/ReflexHost.kt`. Check: `reflex headless`, then `ls` on the
   screen, prints its name, state and actions.
5. **View** in the feature's `-ui` module: a binding composable that collects `state` with
   `collectAsStateWithLifecycle`, and a stateless `XxxContent(state, onAction)` with a `@Preview`. Check: the view
   holds no logic beyond layout; strings exist in `values` and `values-vi`.
6. **Behavior test** in `cli/src/test/.../behavior`: a user journey through `precondition` / `act` / `refused`,
   asserting JSON subsets of state. Every HTTP call needs a fixture in `cli/src/main/resources/fixtures`
   (`FixtureHttp` maps requests to file names). Check: `./gradlew :cli:test` green, and the test goes red when you
   break the behavior it covers.

## Gotchas

- An unmatched HTTP request in a headless run is a test failure (`fixtures.violations`), not a skipped call.
- The introspection server exists only in `app/src/debug`; `app/src/release/.../DebugTools.kt` is its no-op twin.
  Change both signatures together. The abstract socket is reachable by any app on the device: keep it debug-only.
- Room schemas are exported to `core/data/schemas` and committed. A schema change bumps the database version and
  ships a migration; there is no destructive fallback.
- `config/compose/compose_compiler_config.conf` marks domain models and every feature `State` as stable. Keep them
  immutable (read-only `List`, `val` only) or the marking lies to the Compose compiler.
- `ReflexViewModel` effects run on the scope passed in (`Dispatchers.Main.immediate` on Android, one
  `reflex-main` thread headless). Blocking I/O belongs in repositories, which switch to `Dispatchers.IO`.
- `runCatching` around suspend code swallows cancellation: use the `catching*` helpers in `core:data`.
- Koin modules are functions (`fun pokedexModule() = module { }`), not `val`s: a module object holds its singles'
  instances, so two live `ReflexApp`s loading the same `val` (an unclosed headless session and the next one) would
  share one HTTP client and database. `allowOverride(false)` makes a route registered twice fail at startup.

## Commands

- `./gradlew test` runs unit and behavior tests (JUnit 6); `./gradlew :cli:test` runs the behavior tests only.
- `./gradlew spotlessApply` formats (ktlint, 120 columns); `spotlessCheck` runs in verification.
- `./gradlew :cli:installDist`, then `cli/build/install/reflex/bin/reflex headless`, gives an interactive headless app.
