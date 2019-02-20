package deployment.mgmt.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static deployment.mgmt.utils.CollectionUtils.findDuplicates;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionUtilsTest {
    @Test
    public void testFindDuplicates() {
        assertEquals(Set.of(), findDuplicates(List.of("1", "2", "3")));
        assertEquals(Set.of("2"), findDuplicates(List.of("1", "2", "2", "3", "2")));
        assertEquals(Set.of("1", "3"), findDuplicates(List.of("1", "1", "3", "2", "3")));
    }
}