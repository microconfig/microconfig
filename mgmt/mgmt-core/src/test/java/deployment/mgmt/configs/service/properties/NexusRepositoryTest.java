package deployment.mgmt.configs.service.properties;

import org.junit.jupiter.api.Test;

import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.DEPENDENCIES;
import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.SNAPSHOTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NexusRepositoryTest {
    @Test
    public void testRepositoryName() {
        String url = "http://172.30.162.1:80/nexus/content/repositories/snapshots/";
        NexusRepository repository = new NexusRepository("n", url, SNAPSHOTS);
        assertEquals(url.substring(0, url.length() - 1), repository.getUrl());
        assertEquals("snapshots", repository.getRepositoryName());
        assertEquals("http://172.30.162.1:80/nexus", repository.getNexusBaseUrl());

        assertNull(new NexusRepository("n2", "http://172.30.162.1:80/nexus/content/groups/public/", DEPENDENCIES).getRepositoryName());
    }
}