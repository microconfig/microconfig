package io.microconfig.core.properties.repository;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnvProfilesComponentGraphTest {
    ComponentGraph delegate = mock(ComponentGraph.class);
    EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    Environment e = mock(Environment.class);
    Environment p = mock(Environment.class);
    ComponentGraph componentGraph = new EnvProfilesComponentGraph(delegate, environmentRepository);

    @BeforeEach
    void setUp() {
        when(environmentRepository.getOrCreateByName("e")).thenReturn(e);
        when(environmentRepository.getOrCreateByName("p")).thenReturn(p);
        when(e.getProfiles()).thenReturn(singletonList("p"));
        when(p.getProfiles()).thenReturn(emptyList());
    }

    @Test
    public void test() {
        mockGraph("e", "app.yaml", "app.e.yaml");
        mockGraph("p", "app.yaml", "app.p.yaml");
        assertResult("app.yaml", "app.p.yaml", "app.e.yaml");
    }

    @Test
    public void test2() {
        mockGraph("e", "app.yaml", "app.e.p.yaml");
        mockGraph("p", "app.yaml", "app.e.p.yaml");
        assertResult("app.yaml", "app.e.p.yaml");
    }

    @Test
    public void test3() {
        mockGraph("e", "app.yaml", "app.e.e2.e3.p.yaml", "app.e.e2.e3.yaml", "app.e.yaml");
        mockGraph("p", "app.yaml", "app.p.yaml", "app.e.e2.e3.p.yaml");
        assertResult("app.yaml", "app.p.yaml", "app.e.e2.e3.p.yaml", "app.e.e2.e3.yaml", "app.e.yaml");
    }

    @Test
    public void test4() {
        mockGraph("e", "app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.yaml");
        mockGraph("p", "app.yaml", "app.e.p.yaml");
        assertResult("app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.yaml");
    }

    @Test
    public void test5() {
        when(p.getProfiles()).thenReturn(singletonList("z"));
        Environment z = mock(Environment.class);
        when(environmentRepository.getOrCreateByName("z")).thenReturn(z);

        mockGraph("e", "app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.z.yaml", "app.e.yaml");
        mockGraph("p", "app.yaml", "app.e.p.yaml");
        mockGraph("z", "app.yaml", "app.e.z.yaml");
        assertResult("app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.z.yaml", "app.e.yaml");
    }

    private void mockGraph(String env, String... files) {
        when(delegate.getConfigFilesOf("c", env, APPLICATION))
                .thenReturn(files(files));
    }

    private void assertResult(String... names) {
        assertEquals(
                files(names),
                componentGraph.getConfigFilesOf("c", "e", APPLICATION)
        );
    }

    private List<ConfigFile> files(String... names) {
        return stream(names)
                .map(n -> new ConfigFile(new File(n), "ct1", "e"))
                .collect(toList());
    }
}