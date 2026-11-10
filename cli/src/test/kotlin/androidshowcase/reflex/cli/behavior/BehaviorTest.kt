package androidshowcase.reflex.cli.behavior

import androidshowcase.reflex.cli.HeadlessSession
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

/** One fresh headless app per test. A request without a fixture or an unreadable store file fails the test. */
abstract class BehaviorTest {
  protected lateinit var session: HeadlessSession
  protected lateinit var app: ReflexDriver

  @BeforeEach
  fun launch() {
    session = HeadlessSession()
    app = ReflexDriver(session::handle)
  }

  @AfterEach
  fun shutdown() {
    val violations = session.fixtures.violations.toList()
    val storageProblems = session.storageProblems.toList()
    session.close()
    assertTrue(violations.isEmpty()) { "Network requests without a fixture: $violations" }
    assertTrue(storageProblems.isEmpty()) { "Store files the app could not read: $storageProblems" }
  }

  /** First launch up to the loaded Pokédex. */
  protected fun onboard() {
    app.precondition("Language")
    app.act("select", """{"language":"English"}""", state = """{"selected":"English"}""")
    app.act("continue", expect = "Onboarding")
    app.act("finish", expect = "Pokedex", state = """{"isLoading":false}""")
  }
}
