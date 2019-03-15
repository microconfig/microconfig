package io.microconfig.configs.io.ioservice.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.io.ioservice.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
class PropertiesWriter implements ConfigWriter {
    private final File file;

    @Override
    public void write(Map<String, String> properties) {
        doWrite(properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue()));
    }

    @Override
    public void write(Collection<Property> properties) {
        doWrite(properties.stream()
                .filter(p -> !p.isTemp())
                .map(Property::toString));
    }

    @Override
    public void append(Map<String, String> properties) {
        Stream<String> lines = properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue());

        doWrite(concat(of(LINES_SEPARATOR), lines), CREATE, APPEND);
    }

    private void doWrite(Stream<String> lines, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), lines.collect(joining(LINES_SEPARATOR)), openOptions);
    }
}