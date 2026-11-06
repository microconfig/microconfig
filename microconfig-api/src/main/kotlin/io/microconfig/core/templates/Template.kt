package io.microconfig.core.templates

import java.io.File

interface Template {
    val source: File
    val destination: File
    val fileName: String
    val content: String
    val contentAsBytes: ByteArray
}
