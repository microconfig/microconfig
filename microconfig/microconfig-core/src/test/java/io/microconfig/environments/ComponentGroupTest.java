package io.microconfig.environments;

import org.junit.jupiter.api.Test;

import static io.microconfig.environments.Component.byType;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

class ComponentGroupTest {
    private Component payment = byType("payment");
    private Component paymentUi = byType("payment-ui");
    private Component patcher = byType("patcher");

    @Test
    void testConstructorExceptions() {
        assertDoesNotThrow(() -> new ComponentGroup("any", empty(), emptyList(), emptyList(), singletonList(paymentUi)));
        assertDoesNotThrow(() -> new ComponentGroup("any", empty(), emptyList(), singletonList(paymentUi), emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ComponentGroup("any", empty(), singletonList(payment), singletonList(paymentUi), emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ComponentGroup("any", empty(), singletonList(payment), emptyList(), singletonList(paymentUi)));
    }

    @Test
    void testSearchApi() {
        ComponentGroup paymentGroup = paymentGroup();
        assertEquals(of(payment), paymentGroup.getComponentByName(payment.getName()));
        assertEquals(empty(), paymentGroup.getComponentByName("some"));
        assertEquals(asList(payment.getName(), paymentUi.getName()), paymentGroup.getComponentNames());
    }

    @Test
    void testSimpleApi() {
        ComponentGroup paymentGroup = paymentGroup();
        assertEquals("payments", paymentGroup.getName());
        assertEquals(of("4.2.3.4"), paymentGroup.getIp());
        assertEquals(asList(payment, paymentUi), paymentGroup.getComponents());
        assertEquals(emptyList(), paymentGroup.getExcludedComponents());
        assertEquals(emptyList(), paymentGroup.getAppendedComponents());
        assertEquals("payments", paymentGroup.toString());
    }

    @Test
    void testOverride() {
        ComponentGroup original = paymentGroup();
        String newIp = "5.6.7.8";
        assertEquals(of(newIp), original.changeIp(newIp).getIp());

        ComponentGroup afterOverride = original.override(overriddenGroup());
        assertEquals(asList(payment, patcher), afterOverride.getComponents());
        assertEquals(original.getComponents(), afterOverride.override(original).getComponents());
        assertSame(original, original.override(new ComponentGroup("empty", empty(), emptyList(), emptyList(), emptyList())));
    }

    private ComponentGroup paymentGroup() {
        return new ComponentGroup(
                "payments",
                of("4.2.3.4"),
                asList(payment, paymentUi),
                emptyList(),
                emptyList()
        );
    }

    private ComponentGroup overriddenGroup() {
        return new ComponentGroup(
                "payments",
                of("3.8.9.10"),
                emptyList(),
                singletonList(paymentUi),
                singletonList(patcher)
        );
    }
}