package io.microconfig.core.properties

import io.microconfig.core.exceptions.MicroconfigException

class ResolveException
@JvmOverloads
constructor(
    private val current: DeclaringComponent?,
    private val root: DeclaringComponent?,
    message: String,
    cause: Throwable? = null,
) : MicroconfigException(message, cause) {
    var property: Property? = null

    override val message: String
        get() = "Can't build configs for root component '$root'.\n" + problemInfo()

    private fun problemInfo(): String = exceptionInfo() + super.message + "\n" + causeMessage()

    private fun exceptionInfo(): String =
        "Exception in\n\t$current'\n" + (property?.let { "\t$it\n" } ?: "")

    private fun causeMessage(): String =
        when (val cause = cause) {
            null -> ""
            is ResolveException -> "Cause: " + cause.problemInfo()
            else -> "Cause: ${cause.message}"
        }
}
