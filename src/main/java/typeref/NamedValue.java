package typeref;

import java.util.Objects;
import java.util.function.Function;

public interface NamedValue<T> extends MethodFinder, Function<String, T> {
    default String name() {
        checkParametersEnabled();
        return parameter(0).getName();
    }
    default void checkParametersEnabled() {
        if (Objects.equals("arg0", parameter(0).getName())) {
            throw new IllegalStateException("You need to compile with javac -parameters for parameter reflection to work; You also need java 8u60 or newer to use it with lambdas");
        }
    }

    default T value() {
        return apply(name());
    }
}
