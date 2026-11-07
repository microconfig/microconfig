package io.microconfig.core

import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.configtypes.impl.FirstNonEmptyConfigTypeRepository
import io.microconfig.core.configtypes.impl.CustomConfigTypeRepository
import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository
import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.environments.impl.ComponentFactoryImpl
import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.environments.repository.impl.LazyInitEnvRepository
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.impl.PropertiesFactoryImpl
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.io.yaml.YamlResolvers.forValues
import io.microconfig.core.properties.repository.ComponentGraph
import io.microconfig.core.properties.repository.impl.ComponentGraphImpl
import io.microconfig.core.properties.repository.impl.CompositePropertiesRepository
import io.microconfig.core.properties.repository.impl.EnvProfilesComponentGraph
import io.microconfig.core.properties.repository.impl.FilePropertiesRepository
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.impl.MemoizingResolver
import io.microconfig.core.properties.resolvers.impl.PropertyResolutionCache
import io.microconfig.core.properties.resolvers.placeholder.impl.PlaceholderResolver
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.ComponentResolveStrategy
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties.ComponentProperties
import io.microconfig.core.properties.resolvers.placeholder.strategies.composite.impl.CompositeResolveStrategy
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.EnvironmentResolveStrategy
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties.EnvironmentProperties
import io.microconfig.core.properties.resolvers.placeholder.strategies.standard.impl.StandardResolveStrategy
import io.microconfig.core.properties.resolvers.placeholder.strategies.system.impl.SystemResolveStrategy
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import io.microconfig.utils.CacheProxy.cache
import io.microconfig.utils.Logger
import java.io.File

class Microconfig
private constructor(
    val rootDir: File,
    val destinationDir: File,
    val fsReader: FsReader,
    val additionalPlaceholderResolvers: List<PlaceholderResolveStrategy>,
    val additionalPropertiesRepositories: List<PropertiesRepository>,
) {
    val dependencies = Dependencies()

    fun withDestinationDir(destination: File): Microconfig =
        Microconfig(
            rootDir,
            destination,
            fsReader,
            additionalPlaceholderResolvers,
            additionalPropertiesRepositories,
        )

    fun withFsReader(reader: FsReader): Microconfig =
        Microconfig(
            rootDir,
            destinationDir,
            reader,
            additionalPlaceholderResolvers,
            additionalPropertiesRepositories,
        )

    fun withAdditionalPlaceholderResolvers(
        resolvers: List<PlaceholderResolveStrategy>
    ): Microconfig =
        Microconfig(rootDir, destinationDir, fsReader, resolvers, additionalPropertiesRepositories)

    fun withAdditionalPropertiesRepositories(
        repositories: List<PropertiesRepository>
    ): Microconfig =
        Microconfig(rootDir, destinationDir, fsReader, additionalPlaceholderResolvers, repositories)

    fun rootDir(): File = rootDir

    fun destinationDir(): File = destinationDir

    fun fsReader(): FsReader = fsReader

    fun additionalPlaceholderResolvers(): List<PlaceholderResolveStrategy> =
        additionalPlaceholderResolvers

    fun additionalPropertiesRepositories(): List<PropertiesRepository> =
        additionalPropertiesRepositories

    fun dependencies(): Dependencies = dependencies

    fun inEnvironment(name: String): Environment {
        val environment = environments().getByName(name)
        if (environment.isAbstract)
            throw EnvironmentException("Can't build abstract environment $name")
        return environment
    }

    fun environments(): EnvironmentRepository = dependencies.environments

    fun resolver(): Resolver = dependencies.resolver

    fun logger(enabled: Boolean) = Logger.enableLogger(enabled)

    inner class Dependencies {
        private val lazyEnvironments = LazyInitEnvRepository { environments }
        val environments: EnvironmentRepository by lazy(::initEnvironments)
        val componentFactory: ComponentFactory by lazy(::initComponentFactory)
        val configTypeRepository: ConfigTypeRepository by lazy(::initConfigTypeRepository)
        val configIo: ConfigIo by lazy { newConfigIo(fsReader) }
        val propertiesFactory: PropertiesFactory by lazy(::initPropertiesFactory)
        val componentGraph: ComponentGraph by lazy(::initComponentGraph)
        val resolver: Resolver by lazy(::initResolver)

        private fun initEnvironments(): EnvironmentRepository {
            val repository =
                cache<EnvironmentRepository>(
                    FileEnvironmentRepository(
                        rootDir,
                        fsReader,
                        componentFactory,
                        propertiesFactory,
                    )
                )
            lazyEnvironments.delegate = repository
            return repository
        }

        private fun initComponentFactory(): ComponentFactory =
            cache<ComponentFactory>(ComponentFactoryImpl(configTypeRepository, propertiesFactory))

        private fun initConfigTypeRepository(): ConfigTypeRepository =
            cache(
                FirstNonEmptyConfigTypeRepository.composite(
                    CustomConfigTypeRepository.findDescriptorIn(rootDir, fsReader),
                    StandardConfigTypeRepository(),
                )
            )

        private fun initPropertiesFactory(): PropertiesFactory {
            val files = FilePropertiesRepository(componentGraph, lazyEnvironments, configIo)
            val repository =
                cache(
                    CompositePropertiesRepository.compositeOf(
                        additionalPropertiesRepositories,
                        files,
                    )
                )
            return cache<PropertiesFactory>(PropertiesFactoryImpl(repository))
        }

        private fun initComponentGraph(): ComponentGraph =
            EnvProfilesComponentGraph(ComponentGraphImpl.traverseFrom(rootDir), lazyEnvironments)

        fun initResolver(): Resolver {
            val memo = PropertyResolutionCache()
            val expressions = ExpressionResolver()
            val placeholders = initPlaceholderResolver(expressions, memo)
            return cache<Resolver>(MemoizingResolver(forValues(placeholders, expressions), memo))
        }

        private fun initPlaceholderResolver(
            expressions: RecursiveResolver,
            memo: PropertyResolutionCache,
        ): PlaceholderResolver {
            val componentProperties =
                ComponentProperties(componentGraph, environments, rootDir, destinationDir).get()
            val environmentProperties = EnvironmentProperties().get()
            val strategies =
                additionalPlaceholderResolvers +
                    listOf(
                        SystemResolveStrategy.systemPropertiesResolveStrategy(),
                        SystemResolveStrategy.envVariablesResolveStrategy(),
                        ComponentResolveStrategy(componentProperties),
                        EnvironmentResolveStrategy(environments, environmentProperties),
                        StandardResolveStrategy(environments),
                    )
            val strategy = cache(CompositeResolveStrategy.composite(strategies))
            return PlaceholderResolver(
                lazyEnvironments,
                strategy,
                componentProperties.keys + environmentProperties.keys,
                expressions,
                memo,
            )
        }

        fun environments(): EnvironmentRepository = environments

        fun componentFactory(): ComponentFactory = componentFactory

        fun configTypeRepository(): ConfigTypeRepository = configTypeRepository

        fun propertiesFactory(): PropertiesFactory = propertiesFactory

        fun componentGraph(): ComponentGraph = componentGraph

        fun resolver(): Resolver = resolver
    }

    companion object {
        fun searchConfigsIn(rootDir: File): Microconfig {
            val canonical = rootDir.canonicalFile
            require(canonical.exists()) { "Root directory doesn't exist: $rootDir" }
            return Microconfig(
                canonical,
                File(rootDir, "build"),
                DumpedFsReader(),
                emptyList(),
                emptyList(),
            )
        }
    }
}
