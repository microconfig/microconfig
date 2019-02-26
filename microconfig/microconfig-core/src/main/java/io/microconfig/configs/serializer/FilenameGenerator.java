package io.microconfig.configs.serializer;

import java.io.File;

public interface FilenameGenerator {
    File fileFor(String component);
}
