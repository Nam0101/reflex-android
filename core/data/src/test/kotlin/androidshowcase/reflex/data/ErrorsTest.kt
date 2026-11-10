package androidshowcase.reflex.data

import androidshowcase.reflex.domain.error.AppError
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ErrorsTest {
  @Test
  fun `a failure becomes an error value, a result passes through`() {
    assertEquals(1, catchingLocal { 1 }.get())
    assertInstanceOf(AppError.Storage::class.java, catchingLocal { error("disk full") }.getError())
    assertInstanceOf(AppError.Unknown::class.java, catchingRemote { error("bug") }.getError())
  }

  @Test
  fun `cancellation and fatal errors are never turned into values`() {
    assertThrows(CancellationException::class.java) { catchingLocal { throw CancellationException("cancelled") } }
    assertThrows(CancellationException::class.java) { catchingRemote { throw CancellationException("cancelled") } }
    assertThrows(OutOfMemoryError::class.java) { catchingLocal { throw OutOfMemoryError() } }
  }

  @Test
  fun `a failing stream emits its values, then the error, and ends`() = runTest {
    val results = flow {
      emit(1)
      error("database closed")
    }.catchingLocal().toList()

    assertEquals(1, results.first().get())
    assertInstanceOf(AppError.Storage::class.java, results.last().getError())
    assertEquals(2, results.size)
  }
}
