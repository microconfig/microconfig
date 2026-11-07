package io.microconfig

import io.microconfig.CommandLineParamParser.Companion.printErrorAndExit
import java.io.File

class MicroconfigParams(private val parser: CommandLineParamParser) {
    fun rootDir(): File = File(parser.value("r") ?: ".")

    fun destinationDir(): String = parser.value("d") ?: "build"

    fun groups(): List<String> = parser.listValue("g")

    fun services(): List<String> = parser.listValue("s")

    fun stacktrace(): Boolean = parser.booleanValue("stacktrace")

    fun color(): Boolean = parser.valueOr("color", "true") == "true"

    fun version(): Boolean = parser.contains("v")

    fun environments(): Set<String> {
        val environments = parser.listValue("envs").toMutableSet()
        addSingleEnvironment(environments)
        requireEnvironment(environments)
        return environments
    }

    private fun addSingleEnvironment(environments: MutableSet<String>) {
        val environment = parser.value("e") ?: return
        if (environment == "*") printErrorAndExit("use -envs instead of -e to pass `*` as a value")
        environments.add(environment)
    }

    private fun requireEnvironment(environments: Set<String>) {
        if (environments.isEmpty())
            printErrorAndExit("set `-e (environment)` or `-envs (env1),(env2)...`")
    }

    fun isSingleEnvBuild(): Boolean = parser.contains("e") && !parser.contains("envs")

    companion object {
        fun parse(vararg args: String): MicroconfigParams =
            MicroconfigParams(CommandLineParamParser.parse(*args))

        internal fun parse(
            args: Array<out String>,
            beforeValidation: (MicroconfigParams) -> Unit,
        ): MicroconfigParams =
            MicroconfigParams(CommandLineParamParser.parse(args) { beforeValidation(MicroconfigParams(it)) })
    }
}
