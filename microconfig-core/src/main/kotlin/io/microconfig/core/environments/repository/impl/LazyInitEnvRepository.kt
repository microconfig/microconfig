package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository

class LazyInitEnvRepository : EnvironmentRepository {
    lateinit var delegate: EnvironmentRepository

    override fun environments(): List<Environment> = delegate.environments()

    override fun environmentNames(): Set<String> = delegate.environmentNames()

    override fun environmentNames(excluded: Set<String>): Set<String> =
        delegate.environmentNames(excluded)

    override fun profilesOf(name: String): List<String> = delegate.profilesOf(name)

    override fun getByName(name: String): Environment = delegate.getByName(name)

    override fun getOrCreateByName(name: String): Environment = delegate.getOrCreateByName(name)
}
