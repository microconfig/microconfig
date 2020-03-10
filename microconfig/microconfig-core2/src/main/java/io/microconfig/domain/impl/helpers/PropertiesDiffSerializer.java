package io.microconfig.domain.impl.helpers;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.microconfig.service.ioservice.factory.ConfigIoServiceFactory.configIoService;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.warn;
import static java.util.Collections.emptyMap;

class PropertiesDiffSerializer implements Consumer<File> {
    private static final String DIFF_PREFIX = "diff-";

    private Map<String, String> oldProperties;
    private File diffFile;

    @Override
    public void accept(File file) {
        if (oldProperties == null) {
            oldProperties = read(file);
            diffFile = diffFile(file);
            return;
        }

        Map<String, String> diff = compare(oldProperties, read(file));
        if (!diff.isEmpty()) {
            warn("Stored " + diff.size() + " property changes to " + diffFile.getName());
            configIoService().writeTo(diffFile).write(diff);
        }
    }

    private File diffFile(File destination) {
        File diffFile = new File(destination.getParent(), DIFF_PREFIX + destination.getName());
        delete(diffFile);
        return diffFile;
    }

    private Map<String, String> read(File current) {
        try {
            return configIoService().read(current).propertiesAsMap();
        } catch (RuntimeException e) {
            error("Can't read config '" + current + "' for comparison: " + e.getMessage());
            return emptyMap();
        }
    }

    private Map<String, String> compare(Map<String, String> old, Map<String, String> current) {
        if (old.isEmpty()) return emptyMap();

        Map<String, String> result = new TreeMap<>();
        //todo
        return result;
    }
}
