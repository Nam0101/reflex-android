package androidshowcase.reflex.domain.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Every expected failure, as a value. Adapters (network, database, files) classify throwables into
 * one of these; ViewModels put them in state. [cause] is kept for logging and never serialized.
 */
@Serializable
sealed interface AppError {
  val cause: Throwable?

  @Serializable
  @SerialName("Network")
  data class Network(@Transient override val cause: Throwable? = null) : AppError

  @Serializable
  @SerialName("Timeout")
  data class Timeout(@Transient override val cause: Throwable? = null) : AppError

  @Serializable
  @SerialName("NotFound")
  data class NotFound(@Transient override val cause: Throwable? = null) : AppError

  @Serializable
  @SerialName("Server")
  data class Server(val statusCode: Int, @Transient override val cause: Throwable? = null) : AppError

  @Serializable
  @SerialName("Storage")
  data class Storage(@Transient override val cause: Throwable? = null) : AppError

  @Serializable
  @SerialName("Unknown")
  data class Unknown(@Transient override val cause: Throwable? = null) : AppError
}
