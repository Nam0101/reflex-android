package androidshowcase.reflex.cli.behavior

import androidshowcase.reflex.data.local.SETTINGS_FILE_NAME
import androidshowcase.reflex.data.local.StorageProblem
import androidshowcase.reflex.domain.model.AppLanguage
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingBehaviorTest : BehaviorTest() {
  @Test
  fun `first launch picks a language, walks onboarding and lands on the Pokedex, which a relaunch remembers`() {
    app.precondition("Language", """{"selected":null,"canContinue":false,"languages":["English","Vietnamese"]}""")
    app.ignored("continue")
    app.refused("select", """{"language":"Klingon"}""")
    app.act("select", """{"language":"Vietnamese"}""", state = """{"selected":"Vietnamese","canContinue":true}""")
    app.act("continue", expect = "Onboarding", state = """{"page":0,"isLastPage":false}""")
    assertEquals(listOf(AppLanguage.Vietnamese), session.appliedLanguages)

    app.act("next", state = """{"page":1}""")
    app.act("showPage", """{"page":2}""", state = """{"page":2,"isLastPage":true}""")
    app.act("goBack", expect = "Language", state = """{"selected":"Vietnamese"}""")
    app.act("continue", expect = "Onboarding")
    app.act("finish", expect = "Pokedex", state = """{"isLoading":false,"endReached":false}""")
    app.refused("goBack")

    session.relaunch()
    app.precondition("Pokedex", """{"isLoading":false}""")
  }

  @Test
  fun `an unreadable settings file is reported and kept, and the app starts over instead of coming up empty`() {
    onboard()
    val settings = session.dataDir.resolve(SETTINGS_FILE_NAME)
    Files.write(settings, ByteArray(64) { 0xFF.toByte() })

    session.relaunch()
    app.precondition("Language", """{"selected":null}""")
    assertEquals(listOf(StorageProblem.Kind.CorruptFile), session.storageProblems.map { it.kind })
    assertEquals(64, Files.size(settings.resolveSibling("$SETTINGS_FILE_NAME.corrupt")))
    session.storageProblems.clear() // expected here; any other one fails the test
  }
}
