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
  effect; `AppNavigation` in the shell implements those interfaces.
- `update(state, message)` is pure: it returns the next state plus effects (`task`, `stream`, `Effect.Cancel`).
  Every suspend call and side effect runs inside an effect and reports back as a `Message`.
- Errors are values: repositories return `Result<T, AppError>`; `catchingRemote` / `catchingLocal` (and
  `Flow.catchingLocal()`) rethrow `CancellationException` and fatal errors (`nonFatalOrThrow`). State exposes
  `AppError?`; views map it to text with `errorMessage`.
- Dependencies are constructor parameters, wired with the Koin DSL: data and platform ports in
  `shell/DataModule.kt`, screens in each feature's module (`pokedexModule`) with `screen<Route> { }`. ViewModels
  never call Koin, so tests construct them by hand. Each `ReflexApp` owns a `koinApplication`; never `startKoin`.

## Adding a screen

Each step is done when its check holds.

1. **Route**: a `@Serializable` `data object` / `data class` implementing `Route` in the feature's Core module
   (the stack is saved across process death). Check: it compiles without any Compose import.
2. **ViewModel**: extend `ReflexViewModel<State, Action, Message>`. `State` is an immutable `@Serializable` data
   class holding everything the view shows; `Action` is what a user can do (`@Serializable` when it carries input);
   `Message` wraps `ViewAction(action)` plus effect results. Check: `update` is pure (no suspend call, no navigation,
   no service call outside `task` / `stream`), and the behavior test in step 6 drives every `Action`.
3. **Introspection**: annotate the ViewModel with `@Introspected`. KSP generates its node and registers it: one
   action per `Action` subclass, named after it (`ToggleFavorite` → `toggleFavorite`); add `@ActionTitle("…")` when
   the words of the name are not what the user sees. Never write a node by hand. Check: every button or gesture
   in the view is an `Action` subclass, named as a verb.
4. **Register** the screen in two places: `screen<YourRoute> { }` in the feature's Koin module, and `ScreenContent`
   (or `HomeTabs`) in `app/.../ui/ReflexHost.kt`. A new feature also adds its module to `modules(...)` in
   `ReflexApp`, its navigation port to `AppNavigation`, and the KSP plugin with
   `ksp(projects.core.introspectionProcessor)` to its `build.gradle.kts`. Check: `WiringTest` in `:cli:test` is
   green, and `reflex headless`, then `ls` on the screen, prints its name, state and actions.
5. **View** in the feature's `-ui` module: a binding composable that collects `state` with
   `collectAsStateWithLifecycle`, and a stateless `XxxContent(state, onAction)` with a `@Preview`. Check: the view
   holds no logic beyond layout; strings exist in `values` and `values-vi`.
6. **Behavior test** in `cli/src/test/.../behavior`: a user journey through `precondition` / `act` / `ignored` /
   `refused`, asserting JSON subsets of state. `act` fails when the app ignores the action (`update` returned the
   same state and no effect); assert that with `ignored`. Every HTTP call needs a fixture file named after its
   request under `cli/src/main/resources/fixtures` (`FixtureHttp` documents the naming; `reflex headless --record`
   saves one from the real server). Check: `./gradlew :cli:test` green, and the test goes red when you break the
   behavior it covers.

## Gotchas

- An unmatched HTTP request in a headless run is a test failure (`fixtures.violations`), not a skipped call.
  So is a store file the app could not read (`session.storageProblems`).
- Time and chance are services: take `kotlin.time.Clock` / `kotlin.random.Random` through the constructor (Koin
  binds `Platform.clock` and `Platform.random`), never `Clock.System` or `Random.Default` in Core. Behavior tests
  move time with `session.clock.advanceDays(n)`; the headless random is seeded.
- Persisted values that are not rows go in a typed store: `createProtoStore` with a `@Serializable` class whose
  fields carry `@ProtoNumber`s (`SettingsProto` lists the evolution rules). An unreadable file is moved to
  `<file>.corrupt` and reported through `Platform.onStorageProblem`; a format change ships a `DataMigration`.
- `IntrospectionSession.await` without a screen waits for running effects only; a value a stream delivers after
  a write is not counted. In tests, wait for a screen and state (`act`, `precondition`), not for idle.
- The introspection server exists only in `app/src/debug`; `app/src/release/.../DebugTools.kt` is its no-op twin.
  Change both signatures together. The abstract socket is reachable by any app on the device: keep it debug-only.
- Room schemas are exported to `core/data/schemas` and committed. A schema change bumps the database version and
  ships a migration; there is no destructive fallback.
- `config/compose/compose_compiler_config.conf` marks domain models and every feature `State` as stable. Keep them
  immutable (read-only `List`, `val` only) or the marking lies to the Compose compiler.
- `ReflexViewModel` effects run on the scope passed in (`Dispatchers.Main.immediate` on Android, one
  `reflex-main` thread headless). Blocking I/O belongs in repositories, which switch to `Dispatchers.IO`.
- `runCatching` around suspend code swallows cancellation: use the `catching*` helpers in `core:data`.
- After process death only navigation comes back: `MainActivity` saves `ReflexApp.saveNavigation()` (the routes
  over Home and the selected tab) and hands it to `restoreNavigation`. Screens are created again from their
  routes and reload their data, so a route must carry everything its screen needs to start.
- The Android app has one `OkHttpClient` (`ReflexApplication.okHttp`): Ktor uses it with a disk HTTP cache, Coil
  shares its connection pool without that cache. Build it lazily; never touch it on the main thread.
- Koin modules are functions (`fun pokedexModule() = module { }`), not `val`s: a module object holds its singles'
  instances, so two live `ReflexApp`s loading the same `val` (an unclosed headless session and the next one) would
  share one HTTP client and database. `allowOverride(false)` makes a route registered twice fail at startup.

## Commands

- `./gradlew test` runs unit and behavior tests (JUnit 6); `./gradlew :cli:test` runs the behavior tests only.
- `./gradlew spotlessApply` formats (ktlint, 120 columns); `spotlessCheck` runs in verification.
- `./gradlew :cli:installDist`, then `cli/build/install/reflex/bin/reflex headless`, gives an interactive headless
  app: `ls`, `do <action> [json]`, `wait [screen [json]]`, `advance 1d`, `relaunch`. Each command answers once the
  app is idle, so a script can pipe its lines in without sleeping.
- On a device (debug build): `reflex ls|do|wait|screenshot|reset --package <applicationId>`. The CLI runs
  `adb forward` to `<applicationId>.introspection` itself; pass the id of the variant you installed.
