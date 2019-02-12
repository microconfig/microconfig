package deployment.mgmt.atrifacts.changes;

import deployment.mgmt.atrifacts.Artifact;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.joining;

@Getter
@RequiredArgsConstructor
public class ClasspathDiff {
    private final List<File> added;
    private final List<File> removed;
    private final List<File> changed;

    public static ClasspathDiffBuilder builder(String serviceName) {
        return new ClasspathDiffBuilderImpl(serviceName);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return added.size() + removed.size() + changed.size();
    }

    @Override
    public String toString() {
        BiFunction<String, List<File>, String> toString = (state, list) -> {
            if (list.isEmpty()) return "";

            return " " + state + ":\n"
                    + list.stream()
                    .map(f -> "    " + Artifact.fromFile(f).getMavenFormatString() + "\n")
                    .collect(joining());
        };

        return toString.apply("changed", changed)
                + toString.apply("added", added)
                + toString.apply("removed", removed).trim();
    }
}