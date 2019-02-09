package deployment.mgmt.monitoring;

import deployment.mgmt.stat.monitoring.MemoryUsage;
import deployment.util.JsonUtil;
import org.junit.Test;

public class MonitoringServiceImplTest {
    @Test
    public void fromJson() {
        MemoryUsage parse = JsonUtil.parse("{\"name\":\"cr_infra\",\"valueInMb\":2976,\"children\":[{\"name\":\"cr-eureka-alpha\",\"valueInMb\":536,\"children\":[]},{\"name\":\"cr-audit\",\"valueInMb\":477,\"children\":[]},{\"name\":\"cr-users\",\"valueInMb\":414,\"children\":[]},{\"name\":\"cr-auth2\",\"valueInMb\":702,\"children\":[]},{\"name\":\"cr-zuul\",\"valueInMb\":526,\"children\":[]},{\"name\":\"cr-local-zuul\",\"valueInMb\":321,\"children\":[]}]}\n",
                MemoryUsage.class);
    }

}