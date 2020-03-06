package io.microconfig.core.properties.resolver.placeholder;

import org.junit.jupiter.api.Test;

class PlaceholderBordersTest {
    @Test
    void test() {
        PlaceholderBorders borders = PlaceholderBorders.parse(new StringBuilder("hello ${rf} ${c1 ${c2 } @fsd}  ${app::component[dev]@value:${another}#{1+2}} ${}"));
        System.out.println(borders.toPlaceholder("prod"));

        //"${hello@fds${ }

//        PlaceholderBorders borders = PlaceholderBorders.borders("hello ${rf} ${c1 ${c2 } @fsd}  ${app:component@value:${another}#{1+2}} ${}");
        //find @
        //go back. if ${ - is before and path doest contain spaces and }
    }
}