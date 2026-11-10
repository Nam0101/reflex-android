package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.local.FavoriteDao
import androidshowcase.reflex.data.local.FavoriteEntity
import androidshowcase.reflex.domain.model.PokemonSummary
import com.github.michaelbull.result.get
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FavoritesRepositoryImplTest {
  private class FakeDao : FavoriteDao {
    val rows = MutableStateFlow(emptyList<FavoriteEntity>())

    override fun observeAll(): Flow<List<FavoriteEntity>> = rows

    override fun observeExists(id: Int): Flow<Boolean> = rows.map { all -> all.any { it.id == id } }

    override suspend fun upsert(entity: FavoriteEntity) {
      rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun delete(id: Int) {
      rows.value = rows.value.filterNot { it.id == id }
    }
  }

  private lateinit var ioThread: Thread
  private val io = Executors.newSingleThreadExecutor {
    Thread(it, "test-io").also { ioThread = it }
  }.asCoroutineDispatcher()

  /** Threads that built the database, in order. */
  private val builtOn = mutableListOf<Thread>()
  private val repository = FavoritesRepositoryImpl(
    dao = lazy {
      builtOn += Thread.currentThread()
      FakeDao()
    },
    io = io,
    now = { 1L },
  )

  @AfterEach
  fun shutdown() = io.close()

  @Test
  fun `the database is built on the io dispatcher on first use, never by the caller`() = runBlocking {
    val favorites = repository.favorites()
    val isFavorite = repository.isFavorite(25)
    assertEquals(emptyList<Thread>(), builtOn)

    repository.add(PokemonSummary(25, "pikachu", "sprite", "artwork"))

    assertEquals(listOf(ioThread), builtOn)
    assertEquals(listOf(25), favorites.first().get()?.map { it.id })
    assertEquals(true, isFavorite.first().get())
  }
}
