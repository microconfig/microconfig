package deployment.mgmt.atrifacts.changes;

import java.io.File;
import java.util.List;

public interface ClasspathDiffBuilder {
    ClasspathDiffBuilder indexPreviousClasspath(List<File> files);

    ClasspathDiffBuilder indexCurrentClasspath(List<File> files);

    ClasspathDiff compare();
}