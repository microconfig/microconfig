package io.microconfig.core

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.utils.IoUtils.readFully
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MicroconfigRunnerTest {
    @TempDir lateinit var destinationDir: File

    @Test
    fun testBuild() {
        val root: File = classpathFile("repo")
        MicroconfigRunner(root, destinationDir).build("var", emptyList(), emptyList())
        val resultFile: File = File(destinationDir, "var/application.properties")
        assertTrue(resultFile.exists())
        assertEquals("c=3", readFully(resultFile))
    }

    @Test
    fun testBuildAbstract() {
        val root: File = classpathFile("repo")
        val runner: MicroconfigRunner = MicroconfigRunner(root, destinationDir)
        assertThrows(
            EnvironmentException::class.java,
            { runner.build("abstract", emptyList(), emptyList()) },
        )
    }
}
