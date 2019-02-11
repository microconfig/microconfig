package io.microconfig.configs.command.postprocessors;

import io.microconfig.configs.command.PropertiesPostProcessor;
import io.microconfig.configs.properties.Property;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static deployment.util.FileUtils.delete;
import static deployment.util.FileUtils.write;
import static deployment.util.Logger.error;
import static deployment.util.PropertiesUtils.hasTrueValue;

public class WebappPostProcessor implements PropertiesPostProcessor {
    private static final String WEBAPP_FILE = "mgmt.webapp";
    private static final String DEPENDSON_FILE = "mgmt.dependson.list";
    private static final String FORCED_STATUS_FILE = "mgmt.forced.status";

    @Override
    public void process(File serviceDir, String serviceName, Map<String, Property> properties) {
        delete(new File(serviceDir, WEBAPP_FILE));

        if (!hasTrueValue("mgmt.tomcat.webapp.enabled", properties)) return;

        write(new File(serviceDir, WEBAPP_FILE), "");
        delete(new File(serviceDir, DEPENDSON_FILE));
        Property container = properties.get("mgmt.webapp.container");
        if (container == null) {
            error("No container for webapp " + serviceDir.getParentFile().getAbsolutePath());
            return;
        }

        write(new File(serviceDir, DEPENDSON_FILE), container.getValue());
        write(new File(serviceDir, FORCED_STATUS_FILE), "WebApp(" + container + ")");

        File parentFile = canonical(serviceDir);
        String componentDirName = parentFile.getName();
        String contextFileName = "mgmt.tomcat.context." + container + "" + componentDirName + ".xml";
        String contextFile = "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<Context path=\"/" + componentDirName + "\" docBase=\"" + parentFile.getAbsoluteFile() + "/webapp\" />";

        write(new File(serviceDir, contextFileName), contextFile);
    }

    private File canonical(File dir) {
        try {
            return dir.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}