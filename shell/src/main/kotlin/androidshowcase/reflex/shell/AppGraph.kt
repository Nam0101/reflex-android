package androidshowcase.reflex.shell

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.remote.createHttpClient
import androidshowcase.reflex.data.repository.FavoritesRepositoryImpl
import androidshowcase.reflex.data.repository.PokemonRepositoryImpl
import androidshowcase.reflex.data.repository.createSettingsRepository
import androidshowcase.reflex.domain.repository.FavoritesRepository
import androidshowcase.reflex.domain.repository.PokemonRepository
import androidshowcase.reflex.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Composition root of the data layer. Every service is created on first use. */
internal class AppGraph(platform: Platform) : AutoCloseable {
  private val httpClient = lazy { createHttpClient(platform.httpEngine(), logger = platform.httpLogger) }
  private val database = lazy { ReflexDatabase.build(platform.databaseBuilder()) }
  private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val pokemon: PokemonRepository = PokemonRepositoryImpl(httpClient)
  val favorites: FavoritesRepository by lazy { FavoritesRepositoryImpl(database.value.favoriteDao()) }
  val settings: SettingsRepository by lazy { createSettingsRepository(platform.settingsFile, storageScope) }

  override fun close() {
    if (httpClient.isInitialized()) httpClient.value.close()
    if (database.isInitialized()) database.value.close()
    storageScope.cancel()
  }
}
