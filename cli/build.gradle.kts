// Reflex CLI: drives Core without a device. `headless` runs the real app (real ViewModels, navigators,
// repositories, Room, DataStore) with fixtures at the network edge; `ls` / `do` talk to a debug build.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  application
}

application {
  mainClass = "androidshowcase.reflex.cli.MainKt"
  applicationName = "reflex"
  // The bundled SQLite driver loads a native library; newer JDKs block that without this flag.
  applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

dependencies {
  implementation(projects.shell)
  implementation(libs.ktor.client.mock)
}

tasks.named<JavaExec>("run") {
  standardInput = System.`in`
}
