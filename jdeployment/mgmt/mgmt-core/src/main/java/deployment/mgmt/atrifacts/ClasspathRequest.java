package deployment.mgmt.atrifacts;

import deployment.mgmt.configs.service.properties.ProcessProperties;

import java.io.File;
import java.util.List;

public interface ClasspathRequest {
    ClasspathRequest skipIfSnapshot(boolean flag);

    Classpath current();

    Classpath buildUsing(ProcessProperties processProperties);

    interface Classpath {
        String asString();

        List<File> asFiles();
    }
}
