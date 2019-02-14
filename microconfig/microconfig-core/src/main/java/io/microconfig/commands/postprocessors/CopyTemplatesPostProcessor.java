package io.microconfig.commands.postprocessors;

import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.properties.Property;
import io.microconfig.templates.CopyTemplatesService;
import io.microconfig.templates.CopyTemplatesServiceImpl;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.properties.Property.asStringMap;

@RequiredArgsConstructor
public class CopyTemplatesPostProcessor implements PropertiesPostProcessor {
    private final CopyTemplatesService copyTemplatesService;

    @Override
    public void process(File serviceDir, String serviceName, Map<String, Property> properties) {
        copyTemplatesService.copyTemplates(serviceDir, asStringMap(properties));
    }
}