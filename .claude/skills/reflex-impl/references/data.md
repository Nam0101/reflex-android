# Data, ports and use cases

Inside out: the domain declares what it needs, `core:data` implements it, `shell/.../DataModule.kt` binds it,
fixtures make it run headless. `<pkg>` is `androidshowcase.reflex`.

Contents: Domain model and port · HTTP endpoint · Fixtures · Room · Typed store · Platform port · Use case

## Domain model and port

- Model in `core/domain/src/main/kotlin/<pkg>/domain/model/`: `@Serializable data class`, `val` only, read-only
  `List` (it ends up in `State`, and the Compose stability config trusts it is immutable).
- Port in `core/domain/src/main/kotlin/<pkg>/domain/repository/FooRepository.kt`:

```kotlin
interface FooRepository {
  /** Newest first. Emits again on every change. */
  fun observeItems(): Flow<Result<List<FooItem>, AppError>>

  suspend fun item(id: Int): Result<FooItem, AppError>

  suspend fun save(item: FooItem): Result<Unit, AppError>
}
```

One-shot work is `suspend` returning `Result<T, AppError>`; observed data is `Flow<Result<T, AppError>>`. KDoc the
ordering and emission rules the ViewModel relies on. A new failure kind is a new `AppError` subtype plus a branch
in `errorMessage` (`core/designsystem/.../States.kt`) and its strings in both languages.

## HTTP endpoint

`core/data/src/main/kotlin/<pkg>/data/remote/<Api>.kt`, beside `PokeApi.kt`:

```kotlin
internal suspend fun HttpClient.fooItem(id: Int): FooItemDto = get("foo/$id").body()

@Serializable
internal data class FooItemDto(val id: Int, @SerialName("display_name") val displayName: String)

internal fun FooItemDto.toDomain() = FooItem(id = id, name = displayName)
```

Paths are relative: `createHttpClient` sets the base URL, ignores unknown JSON keys, and sets `expectSuccess = true`
so non-2xx responses throw and `catchingRemote` maps them (404 → `NotFound`, other status → `Server`, timeouts →
`Timeout`, I/O → `Network`).

`core/data/src/main/kotlin/<pkg>/data/repository/FooRepositoryImpl.kt`:

```kotlin
/** [client] is lazy and first touched on [io], so building the HTTP stack never costs the main thread. */
class FooRepositoryImpl(
  private val client: Lazy<HttpClient>,
  private val io: CoroutineDispatcher = Dispatchers.IO,
) : FooRepository {
  override suspend fun item(id: Int): Result<FooItem, AppError> = withContext(io) {
    catchingRemote { client.value.fooItem(id).toDomain() }
  }
}
```

Bind in `shell/.../DataModule.kt`: `single<FooRepository> { FooRepositoryImpl(inject()) }` (`inject()` gives the
`Lazy<HttpClient>`). Never `runCatching`: it swallows cancellation.

A second API host needs its own `HttpClient` under a Koin qualifier; its fixtures go in a folder named after the
host.

Unit test in `core/data/src/test/kotlin/<pkg>/data/remote/<Api>Test.kt` with Ktor `MockEngine`, as `PokeApiTest`
does: one test for the DTO → domain mapping, one for status codes → `AppError`.

## Fixtures

Headless runs and behavior tests answer HTTP from files under `cli/src/main/resources/fixtures`, named after the
request. A new request needs a file and no code:

| Request | File |
| --- | --- |
| `GET https://pokeapi.co/api/v2/pokemon/25` | `pokeapi.co/api/v2/pokemon/25.json` |
| `GET …/pokemon?offset=0&limit=20` (query sorted by name) | `pokeapi.co/api/v2/pokemon@limit=20,offset=0.json` |
| `POST …/favorites` (any method but GET) | `pokeapi.co/api/v2/favorites.post.json` |

1. Get the file: `cli/build/install/reflex/bin/reflex headless --record cli/src/main/resources/fixtures`, walk to
   the screen, and the missing responses are saved from the real server. Trim each to the fields the DTO reads.
2. Behavior tests shape the server with `session.fixtures.failOnce("foo/1", HttpStatusCode.X)` and
   `session.fixtures.delay("foo/1", 300.milliseconds)`: the name is the end of the file name, from a `/`.

A request without a file is recorded in `fixtures.violations` (the message names the missing file) and fails the
test even if the app recovered.

## Room

All in `core/data/src/main/kotlin/<pkg>/data/local/` (`ReflexDatabase.kt` holds the current entity and DAO).

1. `@Entity` data class (snake_case `@ColumnInfo` names, indexes for `ORDER BY` columns) and `@Dao` interface:
   reads return `Flow`, writes are `suspend`. Room already runs queries on `Dispatchers.IO` (`ReflexDatabase.build`),
   so repositories do not switch dispatchers for Room.
2. Add the entity to `@Database(entities = [...])` and an `abstract fun fooDao(): FooDao`.
3. Any schema change bumps `version` and ships a migration (an auto-migration for additive changes, otherwise a
   manual migration added in `ReflexDatabase.build`). There is no destructive fallback, on purpose.
4. Build `:core:data` to export `core/data/schemas/<pkg>.data.local.ReflexDatabase/<version>.json`, and commit it
   with the change.
5. Repository:

```kotlin
override fun observeItems(): Flow<Result<List<FooItem>, AppError>> = dao.observeAll()
  .map<List<FooEntity>, Result<List<FooItem>, AppError>> { rows -> Ok(rows.map { it.toDomain() }) }
  .catch { emit(Err(AppError.Storage(it))) }

override suspend fun save(item: FooItem): Result<Unit, AppError> = catchingLocal { dao.upsert(item.toEntity()) }
```

6. Bind: `single<FooRepository> { FooRepositoryImpl(get<ReflexDatabase>().fooDao()) }`. A timestamp comes from the
   app's clock (`get<Clock>()`, bound from `Platform.clock`), as `FavoritesRepositoryImpl`'s `now` does; never
   `System.currentTimeMillis()` inside, or behavior tests cannot move time.

Behavior tests run on a real database file in a temp dir; `session.relaunch()` proves the data survives a restart.

## Typed store (settings and other non-row data)

Stores are DataStore files encoded with kotlinx.serialization ProtoBuf (`core/data/.../local/ProtoStore.kt`).

A new setting: a field in `SettingsProto` (`local/SettingsStore.kt`) with the next free `@ProtoNumber` and a
default, a field in `UserSettings` (`core/domain/.../model/Settings.kt`), mapped in `SettingsRepositoryImpl`, and a
`SettingsRepository` method returning `Result<Unit, AppError>` via
`catchingLocal { dataStore.updateData { it.copy(x = value) } }`. The first emission must be the persisted value,
never a made-up default.

A new store (a profile, a draft): copy `SettingsStore.kt`: an `@Serializable internal data class FooProto` with
`@ProtoNumber`s, `createProtoStore(file = "$directory/foo.pb", FooProto.serializer(), default = FooProto(), scope,
onError)`, and a repository factory taking `(directory, scope, onError)`, bound in `DataModule.kt` with
`platform.storageDir`, `get(StorageScope)` and `platform.onStorageProblem`.

Rules old files depend on: never reuse or renumber a `@ProtoNumber`; a removed field retires its number; a change
ProtoBuf cannot read (a renamed file, another format) ships a `DataMigration` (`LegacySettingsMigration`). A file
that cannot be decoded is moved to `<file>.corrupt`, reported through `Platform.onStorageProblem` (logged on
Android, `session.storageProblems` headless, where an unexpected one fails the behavior test), and the store
continues from its default. Never catch-and-default silently.

## Platform port

For anything that needs `android.*` (locale, notifications, clipboard, permissions):

1. `core/domain/src/main/kotlin/<pkg>/domain/platform/Foo.kt`: a `fun interface`, KDoc saying what Android does and
   what the CLI does (`LanguageApplier`).
2. `shell/.../Platform.kt`: a new constructor property; a `() -> X` factory when creating it is expensive.
3. `shell/.../DataModule.kt`: `single { platform.foo }`.
4. `app/.../ReflexApplication.kt` `androidPlatform`: the real implementation, a private object below it.
5. `cli/.../HeadlessSession.kt` `newApp()`: a fake that records calls in a public `CopyOnWriteArrayList`
   (`appliedLanguages`), so behavior tests assert what reached the platform.

`Platform` is built only in those two places; both must compile.

## Use case

Only for a business rule worth naming and testing on its own (`ResolveLaunchStep`), never a pass-through to one
repository call. Put the pure rule in a function the test calls directly:

```kotlin
class ResolveFoo(private val repository: FooRepository) {
  suspend operator fun invoke(): FooStep = resolve(repository.current())

  companion object {
    fun resolve(value: FooValue): FooStep = when { ... }
  }
}
```

Build it in the Koin registration: `screen<FooRoute> { FooViewModel(ResolveFoo(get()), get(UiScope)) }`.
