package io.microconfig.core.exceptions

open class MicroconfigException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable?) : super(message, cause)
}
