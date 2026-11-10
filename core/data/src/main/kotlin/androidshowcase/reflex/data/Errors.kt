package androidshowcase.reflex.data

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.error.nonFatalOrThrow
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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The error seam of the data layer: runs [block] and turns what it throws into an [AppError] value with
 * [mapError]. Cancellation and fatal errors still propagate, see [nonFatalOrThrow].
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> catching(mapError: (Throwable) -> AppError, block: () -> T): Result<T, AppError> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
  return try {
    Ok(block())
  } catch (t: Throwable) {
    Err(mapError(t.nonFatalOrThrow()))
  }
}

/** Runs a network call; failures are classified by [toRemoteError]. */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> catchingRemote(block: () -> T): Result<T, AppError> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
  return catching(Throwable::toRemoteError, block)
}

/** Runs a database or file operation; any failure is a storage error. */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> catchingLocal(block: () -> T): Result<T, AppError> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
  return catching(AppError::Storage, block)
}

/** A database or file stream as values: a failure is emitted as a storage error and ends the stream. */
internal fun <T> Flow<T>.catchingLocal(): Flow<Result<T, AppError>> =
  map<T, Result<T, AppError>> { Ok(it) }.catch { emit(Err(AppError.Storage(it.nonFatalOrThrow()))) }

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
