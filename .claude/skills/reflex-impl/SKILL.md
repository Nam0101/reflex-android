---
name: reflex-impl
description: Implement app behavior in this reflex-android repo the way its Reflex + Clean Architecture code does it, with templates, the full list of files each change touches, and a verification ladder. Covers a new screen, a new feature module, a repository or HTTP endpoint, a Room table or DataStore key, a platform port, navigation (push, tab, deep link), and their tests (ViewModel unit test, CLI behavior test with fixtures). Use for any change here that adds or changes what the app does, e.g. "thêm màn", "impl tính năng", "làm feature", "add a screen", "call a new endpoint", "lưu vào Room", "add a tab", "deep link". Not for build-logic, CI, release or dependency bumps.
---

# Reflex implementation

AGENTS.md (always in context) holds the layer rules and the short screen recipe. This skill adds the code shapes,
every file a change touches, and how to prove it works. The package root is `androidshowcase.reflex`; if
`scripts/rename-package.sh` has run, use the `namespace` in `app/build.gradle.kts` instead.

## 1. Pick the recipes

| The change needs | Read |
| --- | --- |
| A screen in an existing feature | [references/screen.md](references/screen.md) |
| A new feature (`feature:<name>` + `feature:<name>-ui`) | [references/feature-module.md](references/feature-module.md), then screen.md |
| Data: HTTP endpoint, Room table/column, DataStore key, platform port, use case | [references/data.md](references/data.md) |
| Navigation: push to another screen, a new tab, a deep link | [references/navigation.md](references/navigation.md) |

Most features combine several. Read only the ones the change needs.

## 2. Build inside out

Order: domain model and port → data adapter + fixture → ViewModel (`@Introspected`) + unit test → registration
(Koin, `ReflexHost`) → behavior test → Compose view. Everything before the view runs on the JVM in
seconds, so the behavior is proven before any UI exists; the view only renders state and forwards actions.
Finish each step's check (AGENTS.md lists them) before the next.

## 3. Rules the compiler does not enforce

- `update` can run more than once per message (`MutableStateFlow.update` retries under contention). Build effects
  there; never execute anything there. Navigation, writes, platform calls go inside `task { }`.
- A navigation effect returns no message: `task { navigation.openPokemon(id); null }`.
- Drop stale results: tag the message with what was requested (offset, id, query) and ignore it when state has
  moved on (`PokedexViewModel` `PageLoaded`).
- Ignore repeated submits: return `state.next()` while `isLoading` / `isSaving`, and cover it in a test
  (`app.ignored("retry")`: `send` returns false, and `do` answers `"ignored":true`, when `update` returned the same
  state and no effect).
- After a write, let the observing stream deliver the new data instead of patching state by hand
  (`FavoritesViewModel` `Removed`). Stream keys are private consts; the same key replaces the running stream.
- Values the view or a test reads are in `State`. Derived ones go in the class body (`val endReached = ...`):
  kotlinx.serialization includes them, so `ls` and behavior tests see them.
- Action without input: `data object`. Action with input: `@Serializable data class` (a compile error otherwise).
  `@Introspected` turns each into a CLI action named after it; introspection decodes strictly, so bad input is
  refused before it reaches the ViewModel.
- Time and chance come through the constructor (`kotlin.time.Clock`, `kotlin.random.Random`, bound in Koin from
  `Platform`); behavior tests move time with `session.clock.advanceDays(n)`.
- Everything `State` holds is `@Serializable`, domain models included. `AppError` encodes as
  `{"type":"Server","statusCode":500}`.
- The screen name behavior tests wait for is the ViewModel's class name minus `ViewModel`
  (`@Introspected(name = "…")` overrides it). A tab's name is its route minus `Route`.
- Feature modules never depend on each other. Cross-feature navigation goes through a port the shell implements.
- Views hold no rules: no business `if`, no repository call, no `LaunchedEffect` doing work. A user gesture becomes an
  action (scrolling near the end is `LoadMore`).
- Every user-visible string goes in `values` and `values-vi`, prefixed by screen (`favorites_title`). Errors use
  `errorMessage(error)` from `core:designsystem`, never per-screen error strings.

## 4. Verify

Run from the repo root, cheapest first; fix the first failure before going on.

1. `./gradlew spotlessApply`
2. Unit tests of each touched Core module: `./gradlew :feature:<name>:test :core:data:test` (add `:core:domain:test`,
   `:shell:test` when touched).
3. `./gradlew :cli:test`: behavior journeys, plus `WiringTest` (every `@Introspected` ViewModel is created in a
   Koin `screen<>` and has a view branch in `ReflexHost`; every string has a Vietnamese twin). "Network requests
   without a fixture" names the missing fixture file; it is not a flaky test.
4. `./gradlew :app:assembleDebug`: compiles the `-ui` modules and the host.
5. Red check: break the behavior the new journey covers (return `state.next()` in its branch), run `:cli:test`, see
   it fail at the expected step, revert.
6. Before calling it done, CI's command: `./gradlew spotlessCheck test assembleDebug assembleRelease` (release
   compiles the `DebugTools` no-op twin).

For a manual look: `./gradlew :cli:installDist`, then `cli/build/install/reflex/bin/reflex headless` and type `ls` /
`do <action> {json}` / `wait <screen> {json}` / `advance 1d` / `relaunch`. Each command answers once the app is idle,
so a script can pipe them in; on a device add `--package <applicationId>` (`reflex screenshot`, `reflex reset`).

Report which steps ran and what they printed; name any step skipped.
