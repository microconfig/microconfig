package deployment.configs.properties.files.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public class PropertyFileNameFilter implements Predicate<File> {
    private final String componentType;
    private final String suffix;
    private final int partCount;

    @Override
    public boolean test(File p) {
        String fileName = p.getName();
        return p.getParentFile().getName().equals(componentType)
                && fileName.endsWith("." + suffix)
                && fileName.split("\\.").length == partCount;
    }
}