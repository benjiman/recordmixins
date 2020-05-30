package com.benjiweber.recordmixins;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class RecordMixins {

    private static final List<String> example = List.of("one", "two", "three", "four", "five");

    @Test
    public void map() {
        var mappable = new EnhancedList<>(example);

        assertEquals(
            List.of("oneone", "twotwo", "threethree", "fourfour", "fivefive"),
            mappable.map(s -> s + s)
        );
    }


    @Test
    public void filter() {
        var filterable = new EnhancedList<>(example);

        assertEquals(
            List.of("one", "two"),
            filterable.where(s -> s.length() < 4)
        );
    }

    @Test
    public void group() {
        var groupable = new EnhancedList<>(example);

        assertEquals(
            Map.of(
                3, List.of("one", "two"),
                4, List.of("four", "five"),
                5, List.of("three")
            ),
            groupable.groupBy(String::length)
        );
    }

    @Test
    public void chain_filter() {
        var filterable = new EnhancedList<>(example);

        assertEquals(
            List.of("one"),
            filterable
                .where(s -> s.length() < 4)
                .where(s -> s.endsWith("e"))
        );
    }

    public record EnhancedList<T>(List<T> inner) implements
            ForwardingList<T>,
            Mappable<T>,
            Filterable<T, EnhancedList<T>>,
            Groupable<T> {}

    public interface Mappable<T> extends Forwarding<List<T>> {
        default <R> List<R> map(Function<T, R> f) {
            return inner().stream().map(f).collect(toList());
        }
    }

    public interface Filterable<T, R extends Collection<T>> extends ForwardingAllTheWayDown<List<T>, R> {
        default R where(Predicate<T> p) {
            return forwarding(inner().stream().filter(p).collect(toList()));
        }
    }

    public interface Groupable<T> extends Forwarding<List<T>> {
        default <R> Map<R, List<T>> groupBy(Function<T, R> keyExtractor) {
            return inner().stream().collect(Collectors.groupingBy(keyExtractor));
        }
    }


    interface Forwarding<T> {
        T inner();
    }

    interface ForwardingAllTheWayDown<T, R> extends Forwarding<T> {
        default R forwarding(T t) {
            try {
                return (R) compatibleConstructor(getClass().getConstructors(), t)
                        .newInstance(t);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        default Constructor<?> compatibleConstructor(Constructor<?>[] constructors, T t) {
            return Stream.of(constructors)
                    .filter(ctor -> ctor.getParameterCount() == 1)
                    .filter(ctor -> ctor.getParameters()[0].getType().isAssignableFrom(t.getClass()))
                    .findAny().orElseThrow(IllegalStateException::new);
        }
    }

    interface ForwardingList<T> extends List<T>, Forwarding<List<T>> {
        List<T> inner();

        default int size() {
            return inner().size();
        }

        default boolean isEmpty() {
            return inner().isEmpty();
        }

        default boolean contains(Object o) {
            return inner().contains(o);
        }

        default Iterator<T> iterator() {
            return inner().iterator();
        }

        default Object[] toArray() {
            return inner().toArray();
        }

        default <T1> T1[] toArray(T1[] a) {
            return inner().toArray(a);
        }

        default boolean add(T t) {
            return inner().add(t);
        }

        default boolean remove(Object o) {
            return inner().remove(o);
        }

        default boolean containsAll(Collection<?> c) {
            return inner().containsAll(c);
        }

        default boolean addAll(Collection<? extends T> c) {
            return inner().addAll(c);
        }

        default boolean addAll(int index, Collection<? extends T> c) {
            return inner().addAll(index, c);
        }

        default boolean removeAll(Collection<?> c) {
            return inner().removeAll(c);
        }

        default boolean retainAll(Collection<?> c) {
            return inner().retainAll(c);
        }

        default void replaceAll(UnaryOperator<T> operator) {
            inner().replaceAll(operator);
        }

        default void sort(Comparator<? super T> c) {
            inner().sort(c);
        }

        default void clear() {
            inner().clear();
        }

        default T get(int index) {
            return inner().get(index);
        }

        default T set(int index, T element) {
            return inner().set(index, element);
        }

        default void add(int index, T element) {
            inner().add(index, element);
        }

        default T remove(int index) {
            return inner().remove(index);
        }

        default int indexOf(Object o) {
            return inner().indexOf(o);
        }

        default int lastIndexOf(Object o) {
            return inner().lastIndexOf(o);
        }

        default ListIterator<T> listIterator() {
            return inner().listIterator();
        }

        default ListIterator<T> listIterator(int index) {
            return inner().listIterator(index);
        }

        default List<T> subList(int fromIndex, int toIndex) {
            return inner().subList(fromIndex, toIndex);
        }

        default Spliterator<T> spliterator() {
            return inner().spliterator();
        }


        default <T1> T1[] toArray(IntFunction<T1[]> generator) {
            return inner().toArray(generator);
        }

        default boolean removeIf(Predicate<? super T> filter) {
            return inner().removeIf(filter);
        }

        default Stream<T> stream() {
            return inner().stream();
        }

        default Stream<T> parallelStream() {
            return inner().parallelStream();
        }

        default void forEach(Consumer<? super T> action) {
            inner().forEach(action);
        }
    }


}