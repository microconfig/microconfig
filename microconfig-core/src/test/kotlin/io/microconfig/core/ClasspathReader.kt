package io.microconfig.core

import io.microconfig.utils.IoUtils.readFully
import java.io.File

class ClasspathReader {
    companion object {
        fun classpathFile(name: String): File =
            File(requireNotNull(ClasspathReader::class.java.classLoader.getResource(name)) {
                "Missing classpath resource: $name"
            }.toURI())

        fun read(file: String): String {
            return readFully(classpathFile(file))
        }
    }
}
