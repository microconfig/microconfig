package io.microconfig.core.properties

import java.io.File

data class FileBasedComponent(
    val source: File,
    val lineNumber: Int,
    val isYaml: Boolean,
    override val configType: String,
    override val environment: String,
) : DeclaringComponent {
    override val component: String
        get() = source.parentFile.name

    override fun toString(): String = "${relativeSource()}:${lineNumber + 1}"

    private fun relativeSource(): String {
        val path = source.toString().replace('\\', '/')
        val rootIndex = path.indexOf("/components/")
        return if (rootIndex < 0) path else ".." + path.substring(rootIndex)
    }

    companion object {
        fun fileSource(
            file: File,
            lineNumber: Int,
            yaml: Boolean,
            configType: String,
            environment: String,
        ): FileBasedComponent = FileBasedComponent(file, lineNumber, yaml, configType, environment)
    }
}
