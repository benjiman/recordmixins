package com.benjiweber.recordmixins;

import org.junit.Test;
import typeref.MethodFinder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.benjiweber.recordmixins.DecomposeRecordsTest.If.withFallback;
import static org.junit.Assert.assertEquals;

public class DecomposeRecordsTest {

    record Nums(Integer first, Integer last) {}
    record Colour(Integer r, Integer g, Integer b) {}
    record Wrapper(String value) {}
    record Name(String first, String last) {}
    record NameWithMiddle(String first, String middle, String last) {}

    @Test
    public void decompose_matching_types() {
        AtomicReference<String> result = new AtomicReference<>("Fail");
        Name name = new Name("Benji", "Weber");

        If.<String,String>instance(name, (first, last) -> {
            result.set(first.toLowerCase() + last.toLowerCase());
        });

        assertEquals("benjiweber", result.get());
    }

    @Test
    public void decompose_mismatching_types() {
        AtomicReference<String> result = new AtomicReference<>("Nothing Happened");
        Nums nums = new Nums(5,6);

        If.<String,String>instance(nums, (first, last) -> {
            result.set(first.toLowerCase() + last.toLowerCase());
        });

        assertEquals("Nothing Happened", result.get());
    }

    @Test
    public void decompose_matching_types_to_value() {
        Name name = new Name("Benji", "Weber");

        String result = withFallback("Fail").If.<String,String>instance(name, (first, last) ->
            first.toLowerCase() + last.toLowerCase()
        );

        assertEquals("benjiweber", result);
    }

    @Test
    public void decompose_mismatching_types_to_value() {
        Nums nums = new Nums(5,6);

        String result = withFallback("Nothing").If.<String,String>instance(nums, (first, last) ->
                first.toLowerCase() + last.toLowerCase()
        );

        assertEquals("Nothing", result);
    }

    @Test
    public void decompose_insufficient_arity_to_value() {
        Wrapper wrapper = new Wrapper("Benji");

        String result = withFallback("Nothing").If.<String,String>instance(wrapper, (first, last) ->
                first.toLowerCase() + last.toLowerCase()
        );

        assertEquals("Nothing", result);
    }

    @Test
    public void decompose_surplus_arity_to_value() {
        NameWithMiddle name = new NameWithMiddle("Benji", "???", "Weber");

        String result = withFallback("Fail").If.<String,String>instance(name, (first, middle) ->
                first.toLowerCase() + middle.toLowerCase()
        );

        assertEquals("benji???", result);
    }

    @Test
    public void decompose_matching_types_triple() {
        AtomicReference<Integer> result = new AtomicReference<>(-1);
        Colour c = new Colour(5,6,7);

        If.<Integer,Integer,Integer>instance(c, (r, g, b) -> {
            result.set(r + g + b);
        });

        assertEquals(Integer.valueOf(18), result.get());
    }

    @Test
    public void decompose_matching_types_to_value_triple() {
        Colour c = new Colour(5,6,7);

        int result = withFallback(-1).If.<Integer,Integer,Integer>instance(c, (r, g, b) ->
            r + g + b
        );

        assertEquals(18, result);
    }

    @Test
    public void decompose_mismatching_types_triple() {
        AtomicReference<String> result = new AtomicReference<>("Nothing Happened");
        Colour c = new Colour(5,6,7);

        If.<String,String,String>instance(c, (r, g, b) -> {
            result.set(r.toLowerCase() + r.toLowerCase());
        });

        assertEquals("Nothing Happened", result.get());
    }

    @Test
    public void decompose_mismatching_types_to_value_triple() {
        Colour c = new Colour(5,6,7);

        String result = withFallback("Expected").If.<Integer,String,Integer>instance(c, (r, g, b) ->
                r + g + b
        );

        assertEquals("Expected", result);
    }

    @Test
    public void decompose_supertypes() {
        interface Animal { String noise(); }
        record Duck(String noise) implements Animal {}
        record Dog(String noise) implements Animal {}

        record Zoo(Animal one, Animal two) {}

        Zoo zoo = new Zoo(new Duck("Quack"), new Dog("Woof"));

        String result = withFallback("Fail").If.<Animal,Animal>instance(zoo, (duck, dog) ->
                duck.noise() + dog.noise()
        );

        assertEquals("QuackWoof", result);
    }

    @Test
    public void decompose_subtypes() {
        interface Animal { String noise(); }
        record Duck(String noise) implements Animal {}
        record Dog(String noise) implements Animal {}

        record Zoo(Animal one, Animal two) {}

        Zoo zoo = new Zoo(new Duck("Quack"), new Dog("Woof"));

        String result = withFallback("Fail").If.<Duck,Dog>instance(zoo, (duck, dog) ->
                duck.noise() + dog.noise()
        );

        assertEquals("QuackWoof", result);
    }

    @Test
    public void decompose_subtypes_infer_lhs() {
        interface Animal { String noise(); }
        record Duck(String noise) implements Animal {}
        record Dog(String noise) implements Animal {}

        record Zoo(Animal one, Animal two) {}

        Zoo zoo = new Zoo(new Duck("Quack"), new Dog("Woof"));

        String result = withFallback("Fail").If.instance(zoo, (Duck duck, Dog dog) ->
                duck.noise() + dog.noise()
        );

        assertEquals("QuackWoof", result);
    }

    @Test
    public void decompose_mismatching_subtypes() {
        interface Animal { String noise(); }
        record Duck(String noise) implements Animal {}
        record Dog(String noise) implements Animal {}

        record Zoo(Animal one, Animal two) {}

        Zoo zoo = new Zoo(new Duck("Quack"), new Dog("Woof"));

        String result = withFallback("Fail").If.<Dog,Duck>instance(zoo, (duck, dog) ->
                duck.noise() + dog.noise()
        );

        assertEquals("Fail", result);
    }


    interface ParamTypeAware extends MethodFinder {
        default Class<?> paramType(int n) {
            return method().getParameters()[(actualParamCount() - expectedParamCount()) + n].getType();
        }
        int expectedParamCount();
        private int actualParamCount() {
            return method().getParameters().length;
        }

    }
    interface MethodAwareBiFunction<L,R,TResult> extends BiFunction<L,R,TResult>, ParamTypeAware {
        default Optional<TResult> tryApply(L left, R right) {
            return acceptsTypes(left, right)
                    ? Optional.ofNullable(apply(left, right))
                    : Optional.empty();
        }

        default boolean acceptsTypes(Object left, Object right) {
            return paramType(0).isAssignableFrom(left.getClass())
                    && paramType(1).isAssignableFrom(right.getClass());
        }
        default int expectedParamCount() { return 2; }
    }

    interface MethodAwareBiConsumer<L,R> extends BiConsumer<L,R>, ParamTypeAware {
        default void tryAccept(L left, R right) {
            if (acceptsTypes(left,right)) {
                accept(left, right);
            }
        }

        default boolean acceptsTypes(Object left, Object right) {
            return paramType(0).isAssignableFrom(left.getClass())
                    && paramType(1).isAssignableFrom(right.getClass());
        }
        default int expectedParamCount() { return 2; }
    }

    interface TriFunction<T,U,V,R> {
        R apply(T t, U u, V v);
    }
    interface MethodAwareTriFunction<T,U,V,TResult> extends TriFunction<T,U,V,TResult>, ParamTypeAware {
        default Optional<TResult> tryApply(T one, U two, V three) {
            return acceptsTypes(one, two, three)
                    ? Optional.ofNullable(apply(one, two, three))
                    : Optional.empty();
        }

        default boolean acceptsTypes(Object one, Object two, Object three) {
            return paramType(0).isAssignableFrom(one.getClass())
                    && paramType(1).isAssignableFrom(two.getClass())
                    && paramType(2).isAssignableFrom(three.getClass());
        }
        default int expectedParamCount() { return 3; }
    }

    interface TriConsumer<T,U,V> {
        void accept(T t, U u, V v);
    }
    interface MethodAwareTriConsumer<T,U,V> extends TriConsumer<T,U,V>, ParamTypeAware {
        default void tryAccept(T one, U two, V three) {
            if (acceptsTypes(one, two, three)) {
                accept(one, two, three);
            }
        }

        default boolean acceptsTypes(Object one, Object two, Object three) {
            return paramType(0).isAssignableFrom(one.getClass())
                    && paramType(1).isAssignableFrom(two.getClass())
                    && paramType(2).isAssignableFrom(three.getClass());
        }
        default int expectedParamCount() { return 3; }
    }
    abstract static class Match<TResult> {
        public final Match<TResult> If = this;
        public abstract <L,R> TResult instance(Object toMatch, MethodAwareBiFunction<L,R,TResult> action);
        public abstract <T,U,V> TResult instance(Object toMatch, MethodAwareTriFunction<T,U,V,TResult> action);
    }
    interface If {
        static <TResult> Match<TResult> withFallback(TResult defaultResult) {
            return new Match<>() {
                public <L, R> TResult instance(Object toMatch, MethodAwareBiFunction<L, R, TResult> action) {
                    return DecomposeRecordsTest.If.instance(toMatch, action).orElse(defaultResult);
                }

                public <T, U, V> TResult instance(Object toMatch, MethodAwareTriFunction<T, U, V, TResult> action) {
                    return DecomposeRecordsTest.If.instance(toMatch, action).orElse(defaultResult);
                }
            };
        }
        static <L, R> void instance(Object o, MethodAwareBiConsumer<L, R> action) {
            if (o instanceof Record r) {
                if (r.getClass().getRecordComponents().length < 2) {
                    return;
                }
                action.tryAccept((L) nthComponent(0, r), (R) nthComponent(1, r));
            }
        }
        static <T,U,V> void instance(Object o, MethodAwareTriConsumer<T,U,V> action) {
            if (o instanceof Record r) {
                if (r.getClass().getRecordComponents().length < 3) {
                    return;
                }
                action.tryAccept((T) nthComponent(0, r), (U) nthComponent(1, r), (V) nthComponent(2, r));
            }
        }
        static <L, R, TResult> Optional<TResult> instance(Object o, MethodAwareBiFunction<L, R, TResult> action) {
            if (o instanceof Record r) {
                if (r.getClass().getRecordComponents().length < 2) {
                    return Optional.empty();
                }
                return action.tryApply((L) nthComponent(0, r), (R) nthComponent(1, r));
            }
            return Optional.empty();
        }
        static <T,U,V,TResult> Optional<TResult> instance(Object o, MethodAwareTriFunction<T,U,V,TResult> action) {
            if (o instanceof Record r) {
                if (r.getClass().getRecordComponents().length < 3) {
                    return Optional.empty();
                }
                return action.tryApply((T) nthComponent(0, r), (U) nthComponent(1, r), (V) nthComponent(2, r));
            }
            return Optional.empty();
        }
        private static Object nthComponent(int n, Record r)  {
            try {
                return r.getClass().getRecordComponents()[n].getAccessor().invoke(r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
