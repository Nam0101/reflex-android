package androidshowcase.reflex.shell

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.remote.createHttpClient
import androidshowcase.reflex.data.repository.FavoritesRepositoryImpl
import androidshowcase.reflex.data.repository.PokemonRepositoryImpl
import androidshowcase.reflex.data.repository.createSettingsRepository
import androidshowcase.reflex.domain.repository.FavoritesRepository
import androidshowcase.reflex.domain.repository.PokemonRepository
import androidshowcase.reflex.domain.repository.SettingsRepository
import androidshowcase.reflex.navigation.UiScope
import androidshowcase.reflex.viewmodel.EffectActivity
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

private val StorageScope = named("storageScope")

/**
 * Composition root of the data layer and the platform ports. Every service is created on first use, and
 * closed with the Koin instance that holds it (`onClose` also runs, with null, for services never created).
 */
internal fun dataModule(platform: Platform, effects: EffectActivity): Module = module {
  // ViewModels report their effects to [effects] through the scope they run on.
  single(UiScope) { CoroutineScope(platform.uiScope.coroutineContext + effects) }
  single { platform.languageApplier }
  single { platform.clock }
  single { platform.random }
  single { createHttpClient(platform.httpEngine(), logger = platform.httpLogger) } onClose { it?.close() }
  single { ReflexDatabase.build(platform.databaseBuilder()) } onClose { it?.close() }
  single(StorageScope) { CoroutineScope(SupervisorJob() + Dispatchers.IO) } onClose { it?.cancel() }

  single<PokemonRepository> { PokemonRepositoryImpl(inject()) }
  single<FavoritesRepository> {
    val database = inject<ReflexDatabase>()
    val clock = get<Clock>()
    FavoritesRepositoryImpl(lazy { database.value.favoriteDao() }, now = { clock.now().toEpochMilliseconds() })
  }
  single<SettingsRepository> {
    createSettingsRepository(platform.storageDir, get(StorageScope), platform.onStorageProblem)
  }
}
