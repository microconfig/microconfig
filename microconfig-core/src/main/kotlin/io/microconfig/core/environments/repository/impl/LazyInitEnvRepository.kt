package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository

class LazyInitEnvRepository(private val initialize: (() -> EnvironmentRepository)? = null) :
    EnvironmentRepository {
    @Volatile lateinit var delegate: EnvironmentRepository
    private val initializedDelegate by lazy {
        checkNotNull(initialize) { "Environment repository has no initializer" }().also {
            delegate = it
        }
    }

    override fun environments(): List<Environment> = repository().environments()

    private fun repository(): EnvironmentRepository =
        if (::delegate.isInitialized) delegate else initializedDelegate

    override fun environmentNames(): Set<String> = repository().environmentNames()

    override fun environmentNames(excluded: Set<String>): Set<String> =
        repository().environmentNames(excluded)

    override fun profilesOf(name: String): List<String> = repository().profilesOf(name)

    override fun getByName(name: String): Environment = repository().getByName(name)

    override fun getOrCreateByName(name: String): Environment = repository().getOrCreateByName(name)
}
