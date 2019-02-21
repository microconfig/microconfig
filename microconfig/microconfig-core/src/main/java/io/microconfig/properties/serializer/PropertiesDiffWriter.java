package io.microconfig.properties.serializer;

import io.microconfig.io.ConfigFormat;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static io.microconfig.utils.FileUtils.*;
import static io.microconfig.utils.Logger.warn;

@RequiredArgsConstructor
public class PropertiesDiffWriter implements PropertySerializer {
    private static final String DIFF_PREFIX = "diff-";

    private final PropertySerializer delegate;
    private final ConfigFormat configFormat;

    @Override
    public Optional<File> serialize(String component, Collection<Property> properties) {
        File current = pathFor(component);

        Map<String, String> old = configFormat.read(current);
        Optional<File> result = delegate.serialize(component, properties);

        File diffFile = diffFile(current);
        delete(diffFile);
        String diff = compare(old, properties, component, diffFile);
        if (!diff.isEmpty()) {
            write(diffFile, diff);
        }

        return result;
    }

    private File diffFile(File destination) {
        return new File(destination.getParent(), DIFF_PREFIX + destination.getName());
    }

    private String compare(Map<String, String> old, Collection<Property> current, String component, File diffFile) {
        if (old.isEmpty()) return "";

        StringBuilder content = new StringBuilder(); //todo2 sort by key without +-

        int diffCount = 0;

        for (Property p : current) {
            if (p.isTemp()) continue;

            String oldValue = old.remove(p.getKey());
            if (oldValue == null) {
                if (!p.getSource().isSystem()) {
                    markAdded(p.getKey(), p.getValue(), content);
                    ++diffCount;
                }
            } else if (!p.getValue().equals(oldValue)) {
                markChanged(p.getKey(), oldValue, p.getValue(), content);
                ++diffCount;
            }
        }

        old.forEach((k, oldValue) -> markRemoved(k, oldValue, content));
        diffCount += old.size();

        if (diffCount > 0) {
            warn("Stored " + diffCount + " property changes to " + component + "/" + diffFile.getName());
        }

        return content.toString();
    }

    private void markAdded(String key, String value, StringBuilder content) {
        doWrite("+", key, value, content);
    }

    private void markRemoved(String key, String value, StringBuilder content) {
        doWrite("-", key, value, content);
    }

    private void markChanged(String key, String oldValue, String currentValue, StringBuilder content) {
        doWrite(" ", key, oldValue + " -> " + currentValue, content);
    }

    private void doWrite(String operation, String key, String value, StringBuilder content) {
        content.append(operation)
                .append(key)
                .append("=")
                .append(value)
                .append(LINE_SEPARATOR);
    }

    @Override
    public File pathFor(String component) {
        return delegate.pathFor(component);
    }
}