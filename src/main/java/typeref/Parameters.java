package typeref;

import java.util.function.Consumer;

public interface Parameters<T> extends NewableConsumer<T> {
    default T get() {
        T t = newInstance();
        accept(t);
        return t;
    }

    default void with(Consumer<T> action) {
        action.accept(newInstance());
    }
}