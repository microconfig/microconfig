package io.microconfig.core.properties

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.PropertyImpl.Companion.property
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolveExceptionTest {
    val current: DeclaringComponent = DeclaringComponentImpl("ct2", "current", "e1")

    val root: DeclaringComponent = DeclaringComponentImpl("ct", "root", "e")

    val p: Property = property("key", "value", PROPERTIES, current)

    @Test
    fun withoutCause() {
        val exception: ResolveException =
            ResolveException(current, root, "Can\'t resolve placeholder")
        assertEquals(
            "Can\'t build configs for root component \'root[e]\'.\nException in\n\tcurrent[e1]\'\nCan\'t resolve placeholder",
            exception.message.orEmpty().trim(),
        )
        exception.property = p
        assertEquals(
            "Can\'t build configs for root component \'root[e]\'.\nException in\n\tcurrent[e1]\'\n\tkey=value\nCan\'t resolve placeholder",
            exception.message.orEmpty().trim(),
        )
    }

    @Test
    fun withCauseMessage() {
        val exception: ResolveException =
            ResolveException(
                current,
                root,
                "Can\'t resolve placeholder",
                NullPointerException("NPE"),
            )
        assertEquals(
            "Can\'t build configs for root component \'root[e]\'.\nException in\n\tcurrent[e1]\'\nCan\'t resolve placeholder\nCause: NPE",
            exception.message.orEmpty(),
        )
        exception.property = p
        assertEquals(
            "Can\'t build configs for root component \'root[e]\'.\nException in\n\tcurrent[e1]\'\n\tkey=value\nCan\'t resolve placeholder\nCause: NPE",
            exception.message.orEmpty(),
        )
    }
}
