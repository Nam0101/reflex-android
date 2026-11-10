package androidshowcase.reflex.domain.error

import kotlin.coroutines.cancellation.CancellationException

/** Cancellation and JVM-fatal errors are never business errors: they are rethrown, anything else is returned. */
fun Throwable.nonFatalOrThrow(): Throwable = when (this) {
  is VirtualMachineError, is InterruptedException, is LinkageError, is CancellationException -> throw this
  else -> this
}
