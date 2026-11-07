package io.microconfig

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS

@EnabledOnOs(OS.LINUX, OS.MAC)
class SigningPasswordRotationTest {
    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @ValueSource(strings = ["SIGNING_PASSWORD", "SECRET_PASSPHRASE", ""])
    fun reportGeneratedPasswordsBeforeUpdatingGitHub(failedSecret: String) {
        Files.writeString(directory.resolve("secring.gpg.gpg"), "old-keyring")
        val probe = directory.resolve("rotation-probe.sh")
        Files.writeString(
            probe, """
            source "${'$'}1"
            require_tools() { :; }
            read_passwords() {
              NEW_PASSWORD=test-generated-signing-password
              OLD_PASSPHRASE=test-old-passphrase
              NEW_PASSPHRASE=test-generated-passphrase
            }
            open_workspace() {
              WORK="${'$'}PWD/work"
              mkdir "${'$'}WORK"
              printf new-keyring > "${'$'}WORK/${'$'}KEYRING"
              trap close_workspace EXIT
            }
            close_workspace() { rm -rf "${'$'}WORK"; }
            decrypt_keyring() { :; }
            import_key() { :; }
            find_key() { :; }
            change_key_password() { :; }
            export_key() { :; }
            encrypt_keyring() { :; }
            verify_like_ci() { :; }
            confirm() { return 0; }
            gh() {
              [[ ${'$'}1 == auth ]] && return 0
              grep -q 'test-generated-signing-password' output.log || return 2
              grep -q 'test-generated-passphrase' output.log || return 2
              [[ ${'$'}3 != "${'$'}FAIL_SECRET" ]]
            }
            main
            """.trimIndent()
        )
        val output = directory.resolve("output.log")
        val script = Path.of("../.github/scripts/rotate_signing_passwords.sh").toAbsolutePath()
        val process = ProcessBuilder("bash", probe.toString(), script.toString())
            .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(output.toFile())
            .apply { environment()["FAIL_SECRET"] = failedSecret }.start()

        assertTrue(process.waitFor(10, SECONDS), "Rotation probe timed out")
        val text = Files.readString(output)

        assertEquals(if (failedSecret.isEmpty()) 0 else 1, process.exitValue(), text)
        assertEquals("new-keyring", Files.readString(directory.resolve("secring.gpg.gpg")))
        assertTrue(text.contains("SIGNING_PASSWORD  = test-generated-signing-password"), text)
        assertTrue(text.contains("SECRET_PASSPHRASE = test-generated-passphrase"), text)
        assertEquals(failedSecret.isEmpty(), text.contains("Секреты в GitHub обновлены."), text)
        assertFalse(Files.exists(directory.resolve("work")))
    }
}
