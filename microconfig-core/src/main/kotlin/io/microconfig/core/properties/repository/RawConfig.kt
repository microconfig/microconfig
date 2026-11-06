package io.microconfig.core.properties.repository

import io.microconfig.core.properties.OverrideProperty
import io.microconfig.core.properties.Property
import java.util.function.Function

class RawConfig(
    private val includes: List<Include>,
    private val declaredProperties: List<Property>,
) {
    fun getBaseAndIncludedProperties(
        includeResolver: Function<Include, Map<String, Property>>,
        profiles: List<String>,
        env: String,
    ): Map<String, Property> {
        val properties = filteredProperties(profiles, env)
        mergeIncludes(properties, includeResolver)
        return properties
    }

    private fun filteredProperties(
        profiles: List<String>,
        environment: String,
    ): MutableMap<String, Property> {
        val result = uniqueProperties(declaredProperties.filterNot { it is OverrideProperty }, "")
        applyOverrides(result, " for all envs") { it.isVar && it.isForAllEnvs() }
        (profiles + environment).forEach { overrideFor(result, it) }
        return result
    }

    private fun uniqueProperties(
        properties: List<Property>,
        scope: String,
    ): MutableMap<String, Property> {
        val result = linkedMapOf<String, Property>()
        properties.forEach { property ->
            val previous = result.putIfAbsent(property.key, property)
            check(previous == null) {
                "Property '${property.key}' is declared twice$scope: ${previous?.declaringComponent} and ${property.declaringComponent}"
            }
        }
        return result
    }

    private fun applyOverrides(
        properties: MutableMap<String, Property>,
        scope: String,
        predicate: (OverrideProperty) -> Boolean,
    ) {
        properties.putAll(
            uniqueProperties(
                declaredProperties.filterIsInstance<OverrideProperty>().filter(predicate),
                scope,
            )
        )
    }

    private fun overrideFor(properties: MutableMap<String, Property>, environment: String) {
        val scope = " for env '$environment'"
        applyOverrides(properties, scope) { it.isSharedWith(environment) }
        applyOverrides(properties, scope) { it.isOnlyFor(environment) }
    }

    private fun mergeIncludes(
        properties: MutableMap<String, Property>,
        resolver: Function<Include, Map<String, Property>>,
    ) {
        val included = linkedMapOf<String, Property>()
        includes.forEach { included.putAll(resolver.apply(it)) }
        included.forEach { (key, property) -> properties.putIfAbsent(key, property) }
    }
}
