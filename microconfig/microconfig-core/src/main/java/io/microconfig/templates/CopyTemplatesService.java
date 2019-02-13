package io.microconfig.templates;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(File serviceDir, Map<String, String> serviceProperties);
}
