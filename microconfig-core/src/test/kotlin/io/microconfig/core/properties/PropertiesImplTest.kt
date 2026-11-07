package io.microconfig.core.properties

import io.microconfig.core.properties.impl.PropertiesImpl
import io.microconfig.core.properties.impl.PropertiesImpl.Companion.flat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

class PropertiesImplTest {
    @Test
    fun resolveCustomComponentsOnTheCallingThreadInEncounterOrder() {
        val units = (1..32).map { mock(TypedProperties::class.java) }
        val resolver = mock(Resolver::class.java)
        val caller = Thread.currentThread()
        val visited = mutableListOf<TypedProperties>()
        units.forEach { unit ->
            `when`(unit.resolveBy(resolver)).thenAnswer {
                assertSame(caller, Thread.currentThread())
                visited.add(unit)
                unit
            }
        }
        val resolved = PropertiesImpl(units).resolveBy(resolver)
        assertEquals(units, resolved.asTypedProperties())
        assertEquals(units, visited)
    }

    @Test
    fun stopAtTheFirstCustomResolutionFailureWithoutWrapping() {
        val units = (1..32).map { mock(TypedProperties::class.java) }
        val resolver = mock(Resolver::class.java)
        val visited = AtomicInteger()
        val first = IllegalArgumentException("first component")
        val last = IllegalArgumentException("last component")
        units.forEachIndexed { index, unit ->
            `when`(unit.resolveBy(resolver)).thenAnswer {
                visited.incrementAndGet()
                when (index) {
                    0 -> throw first
                    units.lastIndex -> throw last
                    else -> unit
                }
            }
        }

        val failure = assertThrows(IllegalArgumentException::class.java) {
            PropertiesImpl(units).resolveBy(resolver)
        }

        assertSame(first, failure)
        assertEquals(1, visited.get())
    }

    @Test
    fun invokeCallbacksInOrderOnTheCallingThread() {
        val units = (1..32).map { mock(TypedProperties::class.java) }
        val caller = Thread.currentThread()
        val visited = mutableListOf<TypedProperties>()

        PropertiesImpl(units).forEachComponent { unit ->
            assertSame(caller, Thread.currentThread())
            visited.add(unit)
            unit
        }

        assertEquals(units, visited)
    }

    val tp1: TypedProperties = mock(TypedProperties::class.java)

    val tp2: TypedProperties = mock(TypedProperties::class.java)

    val subj: Properties = PropertiesImpl(listOf(tp1, tp2))

    @Test
    fun compositeFactoryMethod() {
        val prop: Function<TypedProperties, Properties> =
            Function<TypedProperties, Properties> { tp ->
                val p: Properties = mock(Properties::class.java)
                `when`(p.asTypedProperties()).thenReturn(listOf(tp))
                p
            }
        assertEquals(subj, flat(listOf(prop.apply(tp1), prop.apply(tp2))))
    }

    @Test
    fun resolveBy() {
        val r1: TypedProperties = mock(TypedProperties::class.java)
        val r2: TypedProperties = mock(TypedProperties::class.java)
        val resolver: Resolver = mock(Resolver::class.java)
        `when`(tp1.resolveBy(resolver)).thenReturn(r1)
        `when`(tp2.resolveBy(resolver)).thenReturn(r2)
        assertEquals(PropertiesImpl(listOf(r1, r2)), subj.resolveBy(resolver))
    }

    @Test
    fun withoutTempValues() {
        val r1: TypedProperties = mock(TypedProperties::class.java)
        val r2: TypedProperties = mock(TypedProperties::class.java)
        `when`(tp1.withoutVars()).thenReturn(r1)
        `when`(tp2.withoutVars()).thenReturn(r2)
        assertEquals(PropertiesImpl(listOf(r1, r2)), subj.withoutVars())
    }

    @Test
    fun getProperties() {
        val p3: Property = getProperty(3)
        val p1: Property = getProperty(1)
        val p2: Property = getProperty(2)
        `when`(tp1.properties).thenReturn(listOf(p3, p1))
        `when`(tp2.properties).thenReturn(listOf(p2))
        assertEquals(
            listOf(p3, p1, p2).associateBy { it.key },
            subj.propertiesAsMap,
        )
        assertEquals(
            listOf(p3, p1, p2).associate { it.key to it.value },
            subj.propertiesAsKeyValue,
        )
        assertEquals(listOf(p3, p1, p2), subj.properties)
    }

    @Test
    fun getPropertyWithKey() {
        val p1: Property = getProperty(1)
        `when`(tp2.getPropertyWithKey("key1")).thenReturn(Optional.of(p1))
        assertEquals(Optional.of(p1), subj.getPropertyWithKey("key1"))
        assertEquals(Optional.empty<Any>(), subj.getPropertyWithKey("key4"))
    }

    @Test
    fun save() {
        @Suppress("UNCHECKED_CAST")
        val serializer = mock(PropertySerializer::class.java) as PropertySerializer<Int>
        `when`(serializer.prepare(listOf(tp1, tp2))).thenReturn(serializer)
        `when`(tp1.save(serializer)).thenReturn(1)
        `when`(tp2.save(serializer)).thenReturn(2)
        assertEquals(listOf(1, 2), subj.save(serializer))
    }

    @Test
    fun asTypedProperties() {
        assertEquals(listOf(tp1, tp2), subj.asTypedProperties())
        assertEquals(tp1, subj.first())
    }

    private fun getProperty(order: Int): Property {
        val p: Property = mock(Property::class.java)
        `when`(p.key).thenReturn("key" + order)
        `when`(p.value).thenReturn("value" + order)
        return p
    }
}
