package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.io.yaml.impl.SnakeYamlTree

interface YamlTree {
    fun toYaml(flatProperties: Map<String, String>): String

    companion object {
        fun standard(): YamlTree = SnakeYamlTree(true)

        fun compact(): YamlTree = SnakeYamlTree(false)
    }
}
