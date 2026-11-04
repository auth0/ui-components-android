package com.auth0.universalcomponents

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Keeps the packages pointing one way: login hosts MFA, never the other way round, and what both share knows neither.
 * That is what lets MFA run on its own later (step-up) and a new first factor reuse the same building blocks.
 *
 * It reads the source files (a unit test runs from the module's folder), so a new import fails here, not in review.
 */
class ArchitectureTest {

    @Test
    fun `MFA - does not depend on embedded login`() {
        val offenders = sourcesIn(
            "src/main/java/$ROOT/feature/mfachallenge",
            "src/test/java/$ROOT/feature/mfachallenge",
        ).filter { it.mentions("$PACKAGE_ROOT.feature.embedded") }

        assertWithMessage("MFA files that reach into feature.embedded (move what they share to a shared package)")
            .that(offenders.map { it.relativeTo(MODULE).path })
            .isEmpty()
    }

    @Test
    fun `shared building blocks - depend on no feature`() {
        val offenders = sourcesIn(
            "src/main/java/$ROOT/presentation/ui/flow",
            "src/main/java/$ROOT/data/embeddedauth",
            "src/test/java/$ROOT/data/embeddedauth",
        ).filter { it.mentions("$PACKAGE_ROOT.feature.") }
            .map { it.relativeTo(MODULE).path }

        assertWithMessage("Shared files that import a feature (shared code must not know the features that use it)")
            .that(offenders - SESSION_SHARING_TEST)
            .isEmpty()
    }

    private fun sourcesIn(vararg folders: String): List<File> = folders.flatMap { folder ->
        val directory = File(MODULE, folder)
        assertWithMessage("$folder not found: the test must run from the module's folder, or the package moved")
            .that(directory.isDirectory)
            .isTrue()
        directory.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun File.mentions(text: String): Boolean = readLines().any { it.contains(text) }

    private companion object {
        const val PACKAGE_ROOT = "com.auth0.universalcomponents"
        const val ROOT = "com/auth0/universalcomponents"

        /** The one shared test that may name both features: it proves login and MFA can share one SDK session. */
        const val SESSION_SHARING_TEST = "src/test/java/$ROOT/data/embeddedauth/EmbeddedAuthStepsTest.kt"

        /** A Gradle unit test runs with the module's folder as its working directory. */
        val MODULE: File = File("").absoluteFile
    }
}
