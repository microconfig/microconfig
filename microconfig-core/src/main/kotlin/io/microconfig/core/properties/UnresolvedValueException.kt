package io.microconfig.core.properties

import io.microconfig.core.exceptions.MicroconfigException
import io.microconfig.core.properties.repository.ComponentNotFoundException

/** A missing value or a reference cycle can be replaced by an explicit default. */
internal class UnresolvedValueException(message: String) : MicroconfigException(message)

internal fun RuntimeException.isUnresolvedValue(): Boolean =
    when (this) {
        is UnresolvedValueException -> true
        is ComponentNotFoundException -> isDirectLookup
        is ResolveException -> unresolvedValue || (cause as? RuntimeException)?.isUnresolvedValue() == true
        else -> false
    }
