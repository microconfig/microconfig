package deployment.mgmt.version;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static deployment.util.StringUtils.indexOfFirstDigitOr;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class Version {
    private final String value;

    public boolean olderThan(String version) {
        return olderThan(version, false);
    }

    public boolean olderThan(String version, boolean trueIfEquals) {
        int compare = compareVersions(value, version);
        return compare < 0 || compare == 0 && trueIfEquals;
    }

    public List<String> filterNewReleases(Stream<String> allVersions, boolean includeCurrentVersion, int limit) {
        UnaryOperator<String> versionPrefix = v -> v.substring(0, indexOfFirstDigitOr(v, v.length()));
        String currentVersionPrefix = versionPrefix.apply(value);

        return allVersions
                .filter(v -> versionPrefix.apply(v).equals(currentVersionPrefix))
                .filter(v -> olderThan(v, includeCurrentVersion))
                .sorted((v1, v2) -> -1 * compareVersions(v1, v2))
                .limit(limit)
                .collect(toList());
    }

    private int compareVersions(String v1, String v2) {
        if (v1.equals(v2)) return 0;

        Function<String, String[]> parse = s -> s.replaceFirst("^\\w+-", "")
                .replace("-", ".")
                .split("\\.");

        Comparator<String> comparator = comparing(s -> {
            String digits = s.replaceAll("\\D", "");
            return digits.isEmpty() ? 0 : parseInt(digits);
        });

        String[] current = parse.apply(v1);
        String[] other = parse.apply(v2);

        for (int i = 0; i < min(current.length, other.length); ++i) {
            int compareResult = comparator.compare(current[i], other[i]);
            if (compareResult < 0) return -1;
            if (compareResult > 0) return 1;
        }

        return current.length - other.length;
    }
}