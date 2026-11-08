package androidshowcase.reflex.data

import androidshowcase.reflex.domain.error.AppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/** Runs a network call; expected failures become [AppError] values, cancellation still propagates. */
internal inline fun <T> catchingRemote(block: () -> T): Result<T, AppError> = try {
  Ok(block())
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  Err(e.toRemoteError())
}

/** Runs a database or file operation; any failure is a storage error. */
internal inline fun <T> catchingLocal(block: () -> T): Result<T, AppError> = try {
  Ok(block())
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  Err(AppError.Storage(e))
}

internal fun Throwable.toRemoteError(): AppError = when (this) {
  is ClientRequestException -> when (val status = response.status) {
    HttpStatusCode.NotFound -> AppError.NotFound(this)
    else -> AppError.Server(status.value, this)
  }

  is ResponseException -> AppError.Server(response.status.value, this)

  // Timeouts are IOExceptions too: classify them before the generic network case.
  is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> AppError.Timeout(this)

  is IOException -> AppError.Network(this)

  else -> AppError.Unknown(this)
}
