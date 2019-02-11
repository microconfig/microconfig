package deployment.dashboard.env;

import deployment.dashboard.shared.mgmt.MgmtCall;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentMap;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
public class EnvController {
    private final ConcurrentMap<Object, Object> cache = newBuilder()
            .expireAfterWrite(120, SECONDS)
            .build().asMap();

    private final MgmtCall mgmtCall;

    @GetMapping(value = "/memory", produces = APPLICATION_JSON_UTF8_VALUE)
    public String envMemoryStat() {
        return cache.computeIfAbsent("env", k -> envStat()).toString();
    }

    @PostMapping("/refresh")
    public void refresh() {
        cache.clear();
        envMemoryStat();
    }

    private String envStat() {
        return mgmtCall.executeLocally("envMemoryUsageJson");
    }
}