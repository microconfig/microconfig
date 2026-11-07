package io.microconfig.core.environments

import java.io.File
import java.util.Optional

interface Environment {
    val name: String
    val source: File?
    val isAbstract: Boolean
    val profiles: List<String>
    val groups: List<ComponentGroup>
    val allComponents: Components

    fun findGroupsWithIp(ip: String): List<ComponentGroup>

    fun getGroupWithName(groupName: String): ComponentGroup

    fun findGroupWithComponent(componentName: String): Optional<ComponentGroup>

    fun getComponentWithName(componentName: String): Component

    fun findComponentsFrom(groups: List<String>, components: List<String>): Components

    fun findComponentWithName(componentName: String): Component
}
