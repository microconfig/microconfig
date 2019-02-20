package deployment.mgmt.version;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

public class VersionTest {
    @Test
    public void testVersionHighThan() {
        Version version = new Version("2.0.7");
        assertTrue(version.olderThan("2.0.10"));
        assertFalse(version.olderThan("2.0.7"));
        assertFalse(version.olderThan("2.0.6"));

        version = new Version("2.0.10");
        assertFalse(version.olderThan("2.0.10"));
        assertTrue(version.olderThan("2.0.11"));
        assertFalse(version.olderThan("2.0.9"));

        version = new Version("3");
        assertFalse(version.olderThan("2.0.10"));
        assertTrue(version.olderThan("4.11"));
    }

    @Test
    public void filterNewReleases() {
        List<String> allVersions = of("RP-18.22.9", "RP-18.23.1", "RP-18.23.2", "RP-18.24.1", "10.3.4");

        assertEquals(of("RP-18.24.1", "RP-18.23.2", "RP-18.23.1"), new Version("RP-18.23.1").filterNewReleases(allVersions.stream(), true, 5));
        assertEquals(of("RP-18.24.1", "RP-18.23.2", "RP-18.23.1"), new Version("RP-18.23-SNAPSHOT").filterNewReleases(allVersions.stream(), true, 5));
        assertEquals(of(), new Version("RP-18.24-SNAPSHOT").filterNewReleases(Stream.of("RP-17-10.1"), true, 5));
        assertEquals(of(), new Version("RP-18.24.1").filterNewReleases(Stream.of("am-980"), true, 5));
        assertEquals(of(), new Version("18.24.1").filterNewReleases(Stream.of("am-980"), true, 5));
        assertEquals(of("18.24.2", "18.24.1"), new Version("18.24.1").filterNewReleases(Stream.of("17.4", "18.24.0", "18.24.1", "18.24.2"), true, 5));
        assertEquals(of("18.24.2"), new Version("18.24.1").filterNewReleases(Stream.of("17.4", "18.24.0", "18.24.1", "18.24.2"), false, 5));

        assertEquals(of(), new Version("master").filterNewReleases(allVersions.stream(), true, 5));
    }
}