package com.benjiweber.recordmixins;

import org.junit.Test;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class RecordTuplesTest {

    @Test
    public void decomposable_record() {
        Colour colour = new Colour(1,2,3);

        assertEquals(colour.red(), colour.one().intValue());
        assertEquals(colour.green(), colour.two().intValue());
        assertEquals(colour.blue(), colour.three().intValue());

        colour.decompose((r,g,b) -> {
            assertEquals(1, r.intValue());
            assertEquals(2, g.intValue());
            assertEquals(3, b.intValue());
        });

        var sum = colour.decomposeTo((r,g,b) -> r+g+b);
        assertEquals(6, sum.intValue());
    }

    @Test
    public void structural_convert() {
        Colour colour = new Colour(1,2,3);
        Town town = colour.to(Town.class);
        assertEquals(1, town.population());
        assertEquals(2, town.altitude());
        assertEquals(3, town.established());
    }

    @Test
    public void replace_property() {
        Colour colour = new Colour(1,2,3);
        Colour changed = colour.with(Colour::red, 5);
        assertEquals(new Colour(5,2,3), changed);

        Person p1 = new Person("Leslie", 12, 48.3);
        Person p2 = p1.with(Person::name, "Beverly");
        assertEquals(new Person("Beverly", 12, 48.3), p2);
    }

    @Test
    public void auto_builders() {
        Person sam = TriTuple.builder(Person.class)
            .with(Person::name, "Sam")
            .with(Person::age, 34)
            .with(Person::height, 83.2);

        assertEquals(new Person("Sam", 34, 83.2), sam);
    }


    public record Colour(int red, int green, int blue) implements TriTuple<Colour,Integer,Integer,Integer> {}
    public record Person(String name, int age, double height) implements TriTuple<Person, String, Integer, Double> {}
    public record Town(int population, int altitude, int established) implements TriTuple<Town, Integer, Integer, Integer> { }

    public interface MethodAwareFunction<T,R> extends Function<T,R>, MethodFinder { }

    interface TriFunction<T,U,V,R> {
        R apply(T t, U u, V v);
    }

    interface TriConsumer<T,U,V> {
        void apply(T t, U u, V v);
    }

    interface PrimitiveMappings {
        Map<Class<?>, Object> defaultValues = Map.of(
                int.class, 0,
                double.class, 0.0
        );

        Map<Class<?>, Class<?>> boxingMappings = Map.of(
                Integer.class, int.class,
                int.class, Integer.class,
                Double.class, double.class,
                double.class, Double.class
        );

    }

    interface TriTuple<TRecord extends Record & TriTuple<TRecord, T, U, V>,T,U,V> extends DecomposableRecord, PrimitiveMappings {
        default T one() {
            return getComponentValue(0);
        }

        default U two() {
            return getComponentValue(1);
        }

        default V three() {
            return getComponentValue(2);
        }


        default void decompose(TriConsumer<T,U,V> withComponents) {
            withComponents.apply(one(), two(), three());
        }

        default <R> R decomposeTo(TriFunction<T,U,V,R> withComponents) {
            return withComponents.apply(one(), two(), three());
        }

        default <R extends Record & TriTuple<R,T,U,V>> R to(Class<R> cls) {
            try {
                Constructor<?> constructor = findCompatibleConstructorFor(cls);

                return (R)constructor.newInstance(one(), two(), three());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        default <R> TRecord with(MethodAwareFunction<TRecord, R> prop, R newValue) {
            try {
                Class<TRecord> typeParameter = (Class<TRecord>) findTypeParameters()[0];
                Constructor<?> constructor = findCompatibleConstructorFor(typeParameter);
                String propName = prop.method().getName();
                Object[] ctorArgs = Stream.of(0, 1, 2)
                        .map(i -> getComponent(i).replaceIfNamed(propName, newValue))
                        .toArray();
                return (TRecord) constructor.newInstance(ctorArgs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static <T, U, V, TBuild extends Record & TriTuple<TBuild, T, U ,V>> TBuild builder(Class<TBuild> cls) {
            Constructor<?> constructor = Stream.of(cls.getConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 3)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);

            try {
                return (TBuild) constructor.newInstance(
                        defaultValues.get(constructor.getParameters()[0].getType()),
                        defaultValues.get(constructor.getParameters()[1].getType()),
                        defaultValues.get(constructor.getParameters()[2].getType())
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private <R extends Record & TriTuple<R, T, U, V>> Constructor<?> findCompatibleConstructorFor(Class<R> cls) {
            var paramTypes = getParameterTypes();
            return Stream.of(cls.getConstructors())
                    .filter(ctor -> match(ctor.getParameterTypes(), paramTypes))
                    .findFirst().orElseThrow(IllegalStateException::new);
        }

        private boolean match(Class<?>[] constructorParamTypes, List<Class<?>> ourParamTypes) {
            if (constructorParamTypes.length != 3) return false;
            return Stream.of(0,1,2)
                    .allMatch(i -> match(constructorParamTypes[i], ourParamTypes.get(i)));
        }

        private boolean match(Class<?> a, Class<?> b) {
            return a.isAssignableFrom(b) || boxingMappings.getOrDefault(a, a).isAssignableFrom(b);
        }

        private List<Class<?>> getParameterTypes() {
            Type[] types = findTypeParameters();
            return List.of((Class<T>)types[1], (Class<U>)types[2], (Class<V>)types[3]);
        }

        private Type[] findTypeParameters() {
            ParameterizedType triTupleType = Stream.of(getClass().getGenericInterfaces()).map(iface -> (ParameterizedType) iface).filter(iface -> iface.getRawType() == TriTuple.class).findFirst().orElseThrow(IllegalStateException::new);
            return triTupleType.getActualTypeArguments();
        }

    }

    interface MethodFinder extends Serializable {
        default SerializedLambda serialized() {
            try {
                Method replaceMethod = getClass().getDeclaredMethod("writeReplace");
                replaceMethod.setAccessible(true);
                return (SerializedLambda) replaceMethod.invoke(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        default Method method() {
            SerializedLambda lambda = serialized();
            Class<?> containingClass = getContainingClass();
            return Stream.of(containingClass.getDeclaredMethods())
                    .filter(method -> Objects.equals(method.getName(), lambda.getImplMethodName()))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        }

        default Class<?> getContainingClass() {
            try {
                String className = serialized().getImplClass().replaceAll("/", ".");
                return Class.forName(className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    interface DecomposableRecord {
        default <T> T getComponentValue(int index) {
            try {
                return this.<T>getComponent(index).value();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        default <T> NamedProperty<T> getComponent(int index) {
            return new NamedProperty<T>((Record)this, this.getClass().getRecordComponents()[index]);
        }

        record NamedProperty<T>(Record record, RecordComponent component) {
            public T value() {
                try {
                    return (T) component.getAccessor().invoke(record);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public String name() {
                return component.getName();
            }

            public T replaceIfNamed(String propName, T newValue) {
                return Objects.equals(name(), propName)
                        ? newValue
                        : value();
            }
        }


    }

}
