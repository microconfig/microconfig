package io.microconfig.core.properties.serializers;

import com.google.gson.Gson;
import lombok.Data;

import java.util.List;

@Data
public class ConfigResult {
    private final String fileName;
    private final String configType;
    private final String content;

    public static String toJson(List<ConfigResult> configResults) {
        return new Gson().toJson(configResults);
    }
}