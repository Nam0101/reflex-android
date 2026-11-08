package androidshowcase.reflex.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = true)
abstract class ReflexDatabase : RoomDatabase() {
  abstract fun favoriteDao(): FavoriteDao

  companion object {
    const val FILE_NAME = "reflex.db"

    /**
     * Finishes a platform-made builder (Android needs a Context, the CLI builds in memory) the same way
     * everywhere: bundled SQLite, IO dispatcher. Schema changes need a migration: there is no destructive
     * fallback on upgrade, so user data is never dropped silently.
     */
    fun build(builder: Builder<ReflexDatabase>): ReflexDatabase = builder
      .setDriver(BundledSQLiteDriver())
      .setQueryCoroutineContext(Dispatchers.IO)
      .build()
  }
}

@Entity(tableName = "favorites", indices = [Index(value = ["created_at"])])
data class FavoriteEntity(
  @PrimaryKey val id: Int,
  val name: String,
  @ColumnInfo(name = "sprite_url") val spriteUrl: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorites ORDER BY created_at DESC")
  fun observeAll(): Flow<List<FavoriteEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
  fun observeExists(id: Int): Flow<Boolean>

  @Upsert
  suspend fun upsert(entity: FavoriteEntity)

  @Query("DELETE FROM favorites WHERE id = :id")
  suspend fun delete(id: Int)
}
