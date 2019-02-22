package io.microconfig.properties.io;

import io.microconfig.properties.Property;
import io.microconfig.utils.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.properties.Property.typeValue;
import static io.microconfig.properties.io.YamlUtils.asFlatMap;
import static io.microconfig.properties.io.YamlUtils.toTree;
import static io.microconfig.utils.FileUtils.LINE_SEPARATOR;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.emptyMap;
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK;
import static org.yaml.snakeyaml.DumperOptions.LineBreak.WIN;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;

public class YamlConfigIo implements ConfigIo {
    @Override
    public Map<String, String> read(File file) {
        return !file.exists() ? emptyMap() : asFlatMap(file);
    }

    @Override
    public void append(File file, Map<String, String> properties) {
        FileUtils.write(file.toPath(), LINE_SEPARATOR, APPEND);
        doWrite(file, toTree(properties), APPEND);
    }

    @Override
    public void write(File file, Collection<Property> properties) {
        doWrite(file, toTree(properties));
    }

    @Override
    public void write(File file, Map<String, String> properties) {
        doWrite(file, toTree(properties));
    }

    private void doWrite(File file, Map<String, Object> tree, OpenOption... openOptions) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(BLOCK);
        options.setDefaultScalarStyle(PLAIN);
        options.setLineBreak(WIN);
        options.setIndent(2);
        options.setPrettyFlow(true);

        FileUtils.write(file.toPath(), new Yaml(options).dump(tree), openOptions);
    }
}