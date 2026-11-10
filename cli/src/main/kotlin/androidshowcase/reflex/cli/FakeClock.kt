package androidshowcase.reflex.cli

import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** The clock of a headless session: it only moves when a test (or the `advance` command) moves it. */
class FakeClock(start: Instant = Instant.parse("2026-01-01T09:00:00Z")) : Clock {
  private val current = AtomicReference(start)

  override fun now(): Instant = current.get()

  fun advance(duration: Duration) {
    current.updateAndGet { it + duration }
  }

  fun advanceDays(days: Int) = advance(days.days)
}
