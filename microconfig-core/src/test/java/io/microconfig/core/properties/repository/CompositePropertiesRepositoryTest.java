package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.repository.CompositePropertiesRepository.compositeOf;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositePropertiesRepositoryTest {
    PropertiesRepository first = mock(PropertiesRepository.class);
    PropertiesRepository second = mock(PropertiesRepository.class);
    PropertiesRepository subj = compositeOf(singletonList(first), second);

    Property one = property("foo.bar", "value", PROPERTIES, null);
    Property two = property("foo.bar", "default-value", PROPERTIES, null);
    Property three = property("foo.baz", "value", PROPERTIES, null);

    @Test
    public void should_merge_in_order() {
        when(first.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(propsMap(one));
        when(second.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(propsMap(two, three));
        Map<String, Property> expected = propsMap(one, three);

        Map<String, Property> result = subj.getPropertiesOf("component", "dev", APPLICATION);
        assertEquals(expected, result);
    }

    @Test
    public void should_return_empty_map_if_unresolved() {
        when(first.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(emptyMap());
        when(second.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(emptyMap());

        Map<String, Property> result = subj.getPropertiesOf("component", "dev", APPLICATION);
        assertEquals(emptyMap(), result);
    }

    @Test
    public void should_return_same_repository_if_only_one_supplied(){
        PropertiesRepository repo = mock(PropertiesRepository.class);
        assertSame(repo, compositeOf(singletonList(repo)));
        assertSame(repo, compositeOf(emptyList(), repo));
    }

    private Map<String, Property> propsMap(Property... props) {
        return stream(props).collect(toMap(Property::getKey, identity()));
    }

    private Map<String, Property> second() {
        HashMap<String, Property> map = new HashMap<>();
        map.put("foo.bar", two);
        map.put("foo.baz", three);
        return map;
    }

}