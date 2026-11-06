package io.microconfig.core.properties.repository

import io.microconfig.core.exceptions.MicroconfigException
import java.util.ArrayDeque

class ComponentNotFoundException(private val notFoundComponent: String) : MicroconfigException() {
    private val path = ArrayDeque<String>()

    fun withParentComponent(parent: String): ComponentNotFoundException = apply {
        path.addFirst(parent)
    }

    override val message: String
        get() = "Component '$notFoundComponent' doesn't exist." + chainMessage()

    private fun chainMessage(): String =
        if (path.isEmpty()) ""
        else " Dependency chain: ${path.joinToString(" -> ")} -> $notFoundComponent"
}
