package com.benjiweber.recordmixins;

import org.junit.Test;
import typeref.MethodFinder;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static com.benjiweber.recordmixins.OptionalPatternMatchTest.None.*;

public class OptionalPatternMatchTest {

    @Test
    public void traditional_unwrap() {
        Optional<String> unknown = Optional.of("Hello World");
        assertEquals(
                "hello world",
                unknown
                        .map(String::toLowerCase)
                        .orElse("absent")
        );
    }

    @Test
    public void unwrap_optional() {
        Optional<String> unknown = Optional.of("Hello World");
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_empty_optional() {
        Optional<String> unknown = Optional.empty();
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_wrong_type_optional() {
        Optional<Integer> unknown = Optional.of(5);
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_wrong_type_raw_optional() {
        Optional unknown = Optional.of(5);
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_null() {
        Optional<String> unknown = null;
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_unboxed_null() {
        String unknown = null;
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_unboxed_value() {
        String unknown = "Hello World";
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_unboxed_value_unknown_type() {
        Object unknown = "Hello World";
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_unboxed_value_wrong_type() {
        Integer unknown = 5;
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_unboxed_absent_unknown_type() {
        Object unknown = null;
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_object_optional_empty() {
        Object unknown = Optional.empty();
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_object_optional() {
        Object unknown = Optional.of("Hello World");
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }


    @Test
    public void unwrap_raw_optional() {
        Optional unknown = Optional.of("Hello World");
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_empty_raw_optional() {
        Optional unknown = Optional.empty();
        assertEquals(
                "absent",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    @Test
    public void unwrap_optionals_all_the_way_down() {
        var unknown = Optional.of(Optional.of(Optional.of(Optional.of("Hello World"))));
        assertEquals(
                "hello world",
                unwrap(unknown) instanceof String s
                        ? s.toLowerCase()
                        : "absent"
        );
    }

    static Object unwrap(Object o) {
        if (o instanceof Optional<?> opt) {
            return opt.isPresent() ? unwrap(opt.get()) : None;
        } else if (o != null) {
            return o;
        } else {
            return None;
        }
    }
    static class None {
        private None() {}
        public static final None None = new None();
    }


}
