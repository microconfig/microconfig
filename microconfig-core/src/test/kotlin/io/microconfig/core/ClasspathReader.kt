package io.microconfig.core

import io.microconfig.utils.IoUtils.readFully
import java.io.File
import java.io.IOException
import org.springframework.core.io.ClassPathResource

class ClasspathReader {
    companion object {
        fun classpathFile(name: String): File {
            try {
                return ClassPathResource(name).file
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        fun read(file: String): String {
            return readFully(classpathFile(file))
        }
    }
}
