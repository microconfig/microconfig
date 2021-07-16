package io.microconfig.core.properties.resolvers.expression.functions;

import java.io.File;

import static io.microconfig.utils.IoUtils.readAllBytes;
import static io.microconfig.utils.IoUtils.readFully;

public class CustomIoApi {
    public static byte[] readBytes(String path) {
        return readAllBytes(new File(path));
    }

    public static byte[] readBytesOrEmpty(String path) {
        File file = new File(path);
        if (!file.exists()) return new byte[0];
        return readAllBytes(file);
    }

    public static String readString(String path) {
        return readFully(new File(path));
    }

    public static String readStringOrEmpty(String path) {
        File file = new File(path);
        if (!file.exists()) return "";
        return readFully(file);
    }
}