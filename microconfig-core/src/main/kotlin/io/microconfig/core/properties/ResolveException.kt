package io.microconfig.core.properties

import io.microconfig.core.exceptions.MicroconfigException

class ResolveException private constructor(
    private val current: DeclaringComponent?,
    private val root: DeclaringComponent?,
    message: String,
    cause: Throwable?,
    internal val unresolvedValue: Boolean,
) : MicroconfigException(message, cause) {
    @JvmOverloads
    constructor(
        current: DeclaringComponent?,
        root: DeclaringComponent?,
        message: String,
        cause: Throwable? = null,
    ) : this(current, root, message, cause, false)

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

    companion object {
        internal fun unresolved(
            current: DeclaringComponent?,
            root: DeclaringComponent?,
            message: String,
        ): ResolveException = ResolveException(current, root, message, null, true)
    }
}
