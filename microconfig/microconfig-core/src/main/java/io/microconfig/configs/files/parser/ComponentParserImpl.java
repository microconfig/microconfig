package io.microconfig.configs.files.parser;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigReader;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class ComponentParserImpl implements ComponentParser {
    private final String rootComponentDir;
    private final ConfigIoService configIo;

    public ComponentParserImpl(File rootComponentDir, ConfigIoService configIo) {
        this(rootComponentDir.getAbsolutePath(), configIo);
    }

    @Override
    public ParsedComponent parse(File file, Component component, String env) {
        ConfigReader read = configIo.read(file);
        List<Property> properties = read.properties();
        List<String> comments = read.comments();
        List<Include> includes = toIncludes(comments);
        boolean ignore = shouldIgnore(comments);

        return new ParsedComponent(component.getName(), includes, ignore ? emptyList() : properties);
    }

    private List<Include> toIncludes(List<String> comments) {
        return null;
    }

    private boolean shouldIgnore(List<String> comments) {
        return false;
    }

    private boolean isIgnore(String line) {
        return line.startsWith("#@Ignore");
    }

    private String toRelativePath(File path) {
        String absolutePath = path.getAbsolutePath();
        return absolutePath.substring(absolutePath.indexOf(rootComponentDir) + rootComponentDir.length());
    }
}