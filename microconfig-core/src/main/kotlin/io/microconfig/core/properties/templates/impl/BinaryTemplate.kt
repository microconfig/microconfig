package io.microconfig.core.properties.templates.impl

import io.microconfig.core.templates.Template
import io.microconfig.utils.IoUtils.readAllBytes
import java.io.File

class BinaryTemplate
@JvmOverloads
constructor(
    val templateName: String,
    override val source: File,
    override val destination: File,
    override val contentAsBytes: ByteArray = readAllBytes(source),
) : Template {
    override val fileName: String
        get() = destination.name

    override val content: String
        get() = contentAsBytes.toString(Charsets.UTF_8)
}
