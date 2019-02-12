package deployment.mgmt.stat.releases;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static deployment.mgmt.utils.JsonUtil.toJson;
import static io.microconfig.utils.ConsoleColor.green;
import static io.microconfig.utils.Logger.align;

@RequiredArgsConstructor
public class ReadyReleasesImpl implements ReadyReleases {
    private final Map<String, List<String>> releaseInfo;

    public static ReadyReleases of(Map<String, List<String>> releaseInfo) {
        Map<String, List<String>> copy = new LinkedHashMap<>(releaseInfo);
        String key = "";
        List<String> withoutRelease = copy.remove(key);
        if (withoutRelease != null) {
            copy.put(key, withoutRelease);
        }
        return new ReadyReleasesImpl(copy);
    }

    @Override
    public void outputTo(Consumer<String> writeTo) {
        releaseInfo.forEach((entity, values) -> writeTo.accept(consoleFormat(entity, values)));
    }

    @Override
    public void outputAsJsonTo(Consumer<String> writeTo) {
        writeTo.accept(toJson(releaseInfo, true));
    }

    private String consoleFormat(String entity, List<String> values) {
        Supplier<String> formatVersions = () -> {
            int limit = 4;

            if (values.size() <= limit) return values.toString();
            List<String> sublist = new ArrayList<>(values.subList(0, limit));
            sublist.add("...");
            return sublist.toString();
        };

        return align(green(entity), 28) + " -> " + formatVersions.get();
    }
}