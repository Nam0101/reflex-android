package androidshowcase.reflex.viewmodel

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Counts the effects in flight on the UI scope that carries it, so a driver can wait for the app to settle
 * instead of sleeping. A task is busy until it ends (its result is dispatched before that, so a chain of
 * tasks never looks idle in between); a stream is busy until its first value.
 */
class EffectActivity : AbstractCoroutineContextElement(Key) {
  private val running = AtomicInteger()

  val isIdle: Boolean get() = running.get() == 0

  /** Marks one effect as started. Call the result when it is done; calling it again does nothing. */
  internal fun begin(): () -> Unit {
    running.incrementAndGet()
    val done = AtomicBoolean()
    return { if (done.compareAndSet(false, true)) running.decrementAndGet() }
  }

  companion object Key : CoroutineContext.Key<EffectActivity>
}
