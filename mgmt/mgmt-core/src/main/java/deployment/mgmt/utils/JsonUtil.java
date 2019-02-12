package deployment.mgmt.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtil {
    private static final Gson raw = new Gson();

    public static <T> T parse(String json, Class<T> clazz) {
        return raw.fromJson(json, clazz);
    }

    public static String toJson(Object o, boolean prettyOutput) {
        Gson gson = prettyOutput ? new GsonBuilder().setPrettyPrinting().create() : raw;
        return gson.toJson(o);
    }
}