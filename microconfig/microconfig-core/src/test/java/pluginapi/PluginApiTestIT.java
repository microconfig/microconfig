//package pluginapi;
//
//import io.microconfig.commands.buildconfig.factory.ConfigType;
//import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;
//import io.microconfig.commands.buildconfig.factory.StandardConfigType;
//import io.microconfig.configs.ConfigProvider;
//import io.microconfig.configs.Property;
//import io.microconfig.configs.resolver.placeholder.Placeholder;
//import io.microconfig.utils.MicronconfigTestFactory;
//import jdk.tools.jlink.plugin.PluginException;
//
//import java.io.File;
//import java.util.Map;
//import java.util.stream.Stream;
//
//import static io.microconfig.environments.Component.byType;
//import static java.util.Collections.singletonMap;
//import static java.util.Objects.requireNonNull;
//
//public class PluginApiTestIT {
//    public void placeholderValues() {
//        String line = "${ports@p1}";
//        System.out.println(resolve(line, new File("source.yaml")));
//    }
//
//    public Map<String, String> resolve(String placeholder, File currentFile) {
//        MicroconfigFactory factory = MicronconfigTestFactory.getFactory();
//        ConfigProvider configProvider = factory.newConfigProvider(configTypeBy(fileExtension(currentFile.getName())));
//
//        Placeholder p = getPlaceholder(placeholder, currentFile);
//        if (!p.getEnvironment().isEmpty()) {
//            return singletonMap(p.getEnvironment(), doResolve(p, configProvider));
//        } else {
//
//        }
//    }
//
//    private String doResolve(Placeholder p, ConfigProvider configProvider) {
//        Property property = configProvider
//                .getProperties(byType(p.getComponent()), p.getEnvironment())
//                .get(p.getValue());
//
//        requireNonNull(property, () -> "Can't resolve " + p.getValue());
//        return property.getValue();
//    }
//
//    private Placeholder getPlaceholder(String placeholder, File currentFile) {
//        Placeholder p = Placeholder.parse(placeholder, "");
//        return p.getComponent().equals("this") ? p.changeComponent(currentFile.getParentFile().getName()) : p;
//    }
//
//    private ConfigType configTypeBy(String ext) {
//        return Stream.of(StandardConfigType.values())
//                .filter(ct -> ct.getConfigExtensions().stream().anyMatch(e -> e.equals(ext)))
//                .map(StandardConfigType::type)
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("Can't find ConfigType for extension " + ext));
//    }
//
//    private String anyEnv(MicroconfigFactory factory) {
//        return factory.getEnvironmentProvider()
//                .getEnvironmentNames()
//                .stream()
//                .findFirst()
//                .orElse(""); //otherwise will fail for env-specific props
//    }
//
//    public static String fileExtension(String currentFileName) {
//        int lastDot = currentFileName.lastIndexOf('.');
//        if (lastDot >= 0) return currentFileName.substring(lastDot);
//
//        throw new PluginException("Current file doesn't have an extension. Unable to resolve component type.");
//    }
//}
