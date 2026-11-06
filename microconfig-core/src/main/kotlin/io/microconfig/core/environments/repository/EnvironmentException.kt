package io.microconfig.core.environments.repository

import io.microconfig.core.exceptions.MicroconfigException

class EnvironmentException @JvmOverloads constructor(message: String, cause: Throwable? = null) :
    MicroconfigException(message, cause) {
    override val message: String
        get() = super.message + "\n" + cause?.let { "Cause: ${it.message}" }.orEmpty()
}
