package io.microconfig.core.environments

fun interface ComponentFactory {
    fun createComponent(name: String, type: String, environment: String): Component
}
