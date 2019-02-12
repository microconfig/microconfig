package deployment.mgmt.atrifacts;

import deployment.mgmt.atrifacts.strategies.classpathfile.ClasspathFileStrategy;
import deployment.mgmt.configs.service.properties.ClasspathStrategyType;
import deployment.mgmt.configs.service.properties.MavenSettings;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static deployment.mgmt.configs.service.properties.ClasspathStrategyType.NEXUS;
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.Logger.warn;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ClasspathStrategySelectorImpl implements ClasspathStrategySelector {
    private final Map<ClasspathStrategyType, ClasspathStrategy> strategies;

    public static ClasspathStrategySelector from(ClasspathStrategy... strategies) {
        return new ClasspathStrategySelectorImpl(
                of(strategies).collect(toMap(ClasspathStrategy::getType, identity()))
        );
    }

    @Override
    public ClasspathStrategy selectStrategy(String service, MavenSettings mavenSettings) {
        ClasspathStrategy strategy = strategies.get(mavenSettings.getClasspathResolveStrategy());
        info("Resolving dependencies for " + service + " [" + mavenSettings.getArtifact() + "] using " + strategy.getType() + " strategy");
        if (strategy.getType() == NEXUS) {
            warn(NEXUS + " classpath strategy is deprecated. Consider switching to " + ClasspathFileStrategy.class.getSimpleName());
        }
        return strategy;
    }
}
