package androidshowcase.reflex.data.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** What went wrong with a stored file. The store recovered; [cause] and [path] are for logs and reports. */
class StorageProblem(val kind: Kind, val path: String, val cause: Throwable) {
  enum class Kind {
    /** The bytes could not be decoded. They are kept in `<path>.corrupt` and the store restarted from its default. */
    CorruptFile,

    /** A file of an older format could not be imported. It is kept in `<path>.corrupt` and skipped. */
    FailedMigration,

    /** The file could not be read (I/O error); nothing was changed on disk. */
    Unreadable,
  }

  override fun toString() = "StorageProblem($kind, $path, $cause)"
}

/**
 * A typed DataStore encoded with ProtoBuf, the pattern for every store a feature adds. [T] must be an
 * `@Serializable` class whose fields carry `@ProtoNumber`s (see `SettingsProto` for the evolution rules).
 *
 * A file that cannot be decoded is never silently dropped: it is moved to `<file>.corrupt`, [onError] is
 * called and the store continues from [default]. Cancelling [scope] releases the file.
 */
internal fun <T> createProtoStore(
  file: String,
  serializer: KSerializer<T>,
  default: T,
  scope: CoroutineScope,
  onError: (StorageProblem) -> Unit,
  migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T> {
  val path = file.toPath()
  return DataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, ProtoSerializer(serializer, default), producePath = { path }),
    corruptionHandler = ReplaceFileCorruptionHandler { corruption ->
      keepUnreadable(path)
      onError(StorageProblem(StorageProblem.Kind.CorruptFile, file, corruption.cause ?: corruption))
      default
    },
    migrations = migrations,
    scope = scope,
  )
}

/** Moves an unreadable file aside to `<path>.corrupt`, replacing an older one. */
internal fun keepUnreadable(path: Path) {
  FileSystem.SYSTEM.atomicMove(path, "$path.corrupt".toPath())
}

@OptIn(ExperimentalSerializationApi::class)
private class ProtoSerializer<T>(private val serializer: KSerializer<T>, override val defaultValue: T) :
  OkioSerializer<T> {
  override suspend fun readFrom(source: BufferedSource): T {
    val bytes = source.readByteArray()
    return try {
      ProtoBuf.decodeFromByteArray(serializer, bytes)
    } catch (e: Exception) {
      // Bad bytes surface as SerializationException, but also as IndexOutOfBounds and the like.
      throw CorruptionException("Cannot decode ${serializer.descriptor.serialName}", e)
    }
  }

  override suspend fun writeTo(t: T, sink: BufferedSink) {
    sink.write(ProtoBuf.encodeToByteArray(serializer, t))
  }
}
