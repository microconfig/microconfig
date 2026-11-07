package io.microconfig.core.properties.repository

import io.microconfig.core.properties.OverrideProperty
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.yaml.YamlScalars
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
        return mergeIncludes(properties, includeResolver)
    }

    private fun filteredProperties(
        profiles: List<String>,
        environment: String,
    ): Map<String, Property> {
        var result = uniqueProperties(declaredProperties.filterNot { it is OverrideProperty }, "")
        result = applyOverrides(result, " for all envs") { it.isVar && it.isForAllEnvs() }
        for (activeEnvironment in profiles + environment) result = overrideFor(result, activeEnvironment)
        return result
    }

    private fun uniqueProperties(
        properties: List<Property>,
        scope: String,
    ): Map<String, Property> {
        val result = linkedMapOf<String, Property>()
        properties.forEach { property ->
            val previous = result.putIfAbsent(property.key, property)
            check(previous == null) {
                "Property '${property.key}' is declared twice$scope: ${previous?.declaringComponent} and ${property.declaringComponent}"
            }
        }
        return YamlScalars.indexProperties(result.values)
    }

    private fun applyOverrides(
        properties: Map<String, Property>,
        scope: String,
        predicate: (OverrideProperty) -> Boolean,
    ): Map<String, Property> {
        val overrides = uniqueProperties(
            declaredProperties.filterIsInstance<OverrideProperty>().filter(predicate),
            scope,
        )
        if (overrides.isEmpty()) return properties
        return YamlScalars.indexProperties(properties.values + overrides.values)
    }

    private fun overrideFor(properties: Map<String, Property>, environment: String): Map<String, Property> {
        val scope = " for env '$environment'"
        val shared = applyOverrides(properties, scope) { it.isSharedWith(environment) }
        return applyOverrides(shared, scope) { it.isOnlyFor(environment) }
    }

    private fun mergeIncludes(
        properties: Map<String, Property>,
        resolver: Function<Include, Map<String, Property>>,
    ): Map<String, Property> {
        if (includes.isEmpty()) return properties
        val included = YamlScalars.indexProperties(includes.flatMap { resolver.apply(it).values })
        return YamlScalars.indexProperties(properties.values + included.values, overwrite = false)
    }
}
