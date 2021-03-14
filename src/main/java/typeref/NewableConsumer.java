package typeref;

import java.util.function.Consumer;

public interface NewableConsumer<T> extends Consumer<T>, Newable<T> {

    default boolean canCast(Object o) {
        T t = (T)o;

        return true;
    }

}
