//package io.microconfig.core.properties.templates;
//
//import io.microconfig.core.properties.Property;
//import lombok.RequiredArgsConstructor;
//
//import java.io.File;
//import java.util.Map;
//
//
//@RequiredArgsConstructor
//public class CopyTemplatesPostProcessor implements BuildConfigPostProcessor {
//    private final CopyTemplatesService copyTemplatesService;
//
//    @Override
//    public void process(EnvComponent currentComponent,
//                        Map<String, Property> componentProperties,
//                        ConfigProvider configProvider, File resultFile) {
//        if (configProvider instanceof PropertyResolverHolder) {
//            PropertyResolver resolver = ((PropertyResolverHolder) configProvider).getResolver();
//            copyTemplatesService.copyTemplates(currentComponent, resultFile.getParentFile(), asStringMap(componentProperties), resolver);
//        }
//    }
//}