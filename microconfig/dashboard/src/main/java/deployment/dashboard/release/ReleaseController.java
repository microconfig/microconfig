package deployment.dashboard.release;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class ReleaseController {
    private final ReleaseService releaseService;

    @GetMapping(value = "/current-release/{group}", produces = APPLICATION_JSON_UTF8_VALUE)
    public String currentRelease(@PathVariable("group") String group) {
        return releaseService.currentRelease(group);
    }

    @GetMapping(value = "/new-releases/{group}", produces = APPLICATION_JSON_UTF8_VALUE)
    public String newReleases(@PathVariable("group") String group) {
        return releaseService.newReleases(group);
    }

    @PostMapping("/approve/{group}")
    public String approve(@PathVariable("group") String group,
                          @RequestBody Map<String, List<String>> releaseToServices) {
        return releaseService.approve(group, releaseToServices);
    }

    @PostMapping("deploy/{group}")
    public void deploy(@PathVariable("group") String group,
                       @RequestBody Map<String, List<String>> releaseToServices,
                       HttpServletResponse response) throws IOException {
        releaseService.deploy(group, releaseToServices, response.getOutputStream());
    }

    @RequestMapping("/healthcheck/{group}")
    public void healthcheck(@PathVariable("group") String group,
                            HttpServletResponse response) throws IOException {
        releaseService.healthcheck(group, response.getOutputStream());
    }

    @RequestMapping("/status/{group}")
    public void status(@PathVariable("group") String group,
                       HttpServletResponse response) throws IOException {
        releaseService.status(group, response.getOutputStream());
    }

    @GetMapping("/release-history/{group}")
    public String releaseHistory(@PathVariable("group") String group) {
        return releaseService.releaseHistory(group);
    }
}