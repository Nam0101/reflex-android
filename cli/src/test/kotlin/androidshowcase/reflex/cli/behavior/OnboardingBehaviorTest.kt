package androidshowcase.reflex.cli.behavior

import androidshowcase.reflex.domain.model.AppLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingBehaviorTest : BehaviorTest() {
  @Test
  fun `first launch picks a language, walks onboarding and lands on the Pokedex, which a relaunch remembers`() {
    app.precondition("Language", """{"selected":null,"canContinue":false,"languages":["English","Vietnamese"]}""")
    app.act("continue", state = """{"selected":null,"isSaving":false}""")
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
}
