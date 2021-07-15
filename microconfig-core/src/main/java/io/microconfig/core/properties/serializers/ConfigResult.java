package io.microconfig.core.properties.serializers;

import com.google.gson.Gson;
import io.microconfig.core.properties.templates.BinaryTemplate;
import io.microconfig.core.templates.Template;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.flatMapEach;
import static io.microconfig.utils.StreamUtils.forEach;
import static io.microconfig.utils.StringUtils.isEmpty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Data
public class ConfigResult {
    private final String component;
    private final String configType;
    private final String fileName;
    private final String content;
    private final List<Template> templates;

    public static String toJson(List<ConfigResult> configResults) {
        return new Gson().toJson(groupsByService(configResults));
    }

    public static List<ServiceConfigs> groupsByService(List<ConfigResult> configResults) {
        return configResults.stream()
                .filter(c -> !isEmpty(c.getContent()))
                .collect(groupingBy(ConfigResult::getComponent))
                .entrySet().stream()
                .map(e -> new ServiceConfigs(e.getKey(), e.getValue()))
                .collect(toList());
    }

    @Getter
    public static class ServiceConfigs {
        private final String service;
        private final List<FileResult> files;

        ServiceConfigs(String service, List<ConfigResult> configResults) {
            this.service = service;
            this.files = forEach(configResults, FileResult::new);
            files.addAll(forEach(flatMapEach(configResults, ConfigResult::getTemplates), FileResult::new));
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class FileResult {
        private final String configType;
        private final String fileName;
        private final Object content;

        FileResult(ConfigResult configResult) {
            this(configResult.getConfigType(), configResult.getFileName(), configResult.getContent());
        }

        public FileResult(Template template) {
            this("template", template.getFileName(),
                    template instanceof BinaryTemplate ? template.getContentAsBytes() : template.getContent()
            );
        }
    }
}