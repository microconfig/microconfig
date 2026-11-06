package io.microconfig.core.properties

interface DeclaringComponent {
    val configType: String
    val component: String
    val environment: String
}
