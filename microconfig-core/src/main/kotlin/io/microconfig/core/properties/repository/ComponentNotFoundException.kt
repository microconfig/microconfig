package io.microconfig.core.properties.repository

import io.microconfig.core.exceptions.MicroconfigException
import java.util.*

class ComponentNotFoundException(private val notFoundComponent: String) : MicroconfigException() {
    private val path = ArrayDeque<String>()

    internal val isDirectLookup: Boolean
        get() = path.isEmpty()

    fun withParentComponent(parent: String): ComponentNotFoundException = apply {
        path.addFirst(parent)
    }

    override val message: String
        get() = "Component '$notFoundComponent' doesn't exist." + chainMessage()

    private fun chainMessage(): String =
        if (path.isEmpty()) ""
        else " Dependency chain: ${path.joinToString(" -> ")} -> $notFoundComponent"
}
