package io.microconfig.environments.filebased.parsers;

import com.google.gson.Gson;

import java.util.Map;

public class JsonEnvironmentParser extends AbstractEnvironmentParser {
    private final Gson gson = new Gson();

    @Override
    protected Map<String, Object> toMap(String content) {
        return gson.fromJson(content, Map.class);
    }
}