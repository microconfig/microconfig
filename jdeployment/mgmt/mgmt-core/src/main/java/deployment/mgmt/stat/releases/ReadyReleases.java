package deployment.mgmt.stat.releases;

import java.util.function.Consumer;

public interface ReadyReleases {
    void outputTo(Consumer<String> writeTo);

    void outputAsJsonTo(Consumer<String> writeTo);
}
