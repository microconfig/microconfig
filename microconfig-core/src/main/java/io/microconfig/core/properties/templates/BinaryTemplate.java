package io.microconfig.core.properties.templates;

import io.microconfig.core.templates.Template;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.IoUtils.readAllBytes;

@Getter
@RequiredArgsConstructor
public class BinaryTemplate implements Template {
    private final String templateName;
    private final File source;
    private final File destination;
    private final byte[] contentAsBytes;

    public BinaryTemplate(String templateName, File source, File destination) {
        this.templateName = templateName;
        this.source = source;
        this.destination = destination;
        this.contentAsBytes = readAllBytes(source);
    }

    @Override
    public String getFileName() {
        return destination.getName();
    }

    @Override
    public String getContent() {
        return new String(contentAsBytes);
    }
}