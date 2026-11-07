package io.microconfig

import io.microconfig.core.MicroconfigRunner
import io.microconfig.utils.IoUtils.readClasspathResource
import io.microconfig.utils.Logger.announce
import io.microconfig.utils.Logger.error
import io.microconfig.utils.Logger.info
import java.io.File
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.system.exitProcess

fun main(args: Array<String>) = MicroconfigMain.main(*args)

class MicroconfigMain(
    private val rootDir: File,
    private val destinationDir: String,
    private val environments: Set<String>,
    private val groups: List<String>,
    private val services: List<String>,
    private val stacktrace: Boolean,
    private val isSingleEnvBuild: Boolean,
) {
    private fun build() {
        try {
            buildEnvironments()
        } catch (failure: RuntimeException) {
            val message = failure.message
            if (stacktrace || message == null) throw failure
            error(message)
            exitProcess(-1)
        }
    }

    private fun buildEnvironments() {
        environmentsToBuild().forEach(::buildEnvironment)
    }

    private fun environmentsToBuild(): Set<String> {
        val excluded =
            environments.filter { it.startsWith("!") }.map { it.removePrefix("!") }.toSet()
        if ("*" !in environments)
            return environments.filterNot { it.startsWith("!") || it in excluded }.toSet()
        return MicroconfigRunner(rootDir, File(destinationDir))
            .microconfig
            .environments()
            .environmentNames(excluded)
    }

    private fun buildEnvironment(environment: String) {
        val started = System.nanoTime()
        val destination = if (isSingleEnvBuild) destinationDir else "$destinationDir/$environment"
        MicroconfigRunner(rootDir, File(destination)).build(environment, groups, services)
        announce(
            "\nGenerated [$environment] configs in ${NANOSECONDS.toMillis(System.nanoTime() - started)}ms"
        )
    }

    companion object {
        fun main(vararg args: String) {
            val params = MicroconfigParams.parse(*args)
            if (params.version()) {
                printVersion()
                return
            }
            buildFrom(params)
        }

        private fun printVersion() {
            val version =
                readClasspathResource("version.properties").split('\n')[0].split('=')[1].trim()
            info(version)
        }

        private fun buildFrom(params: MicroconfigParams) {
            MicroconfigMain(
                    params.rootDir(),
                    params.destinationDir(),
                    params.environments(),
                    params.groups(),
                    params.services(),
                    params.stacktrace(),
                    params.isSingleEnvBuild(),
                )
                .build()
        }
    }
}
