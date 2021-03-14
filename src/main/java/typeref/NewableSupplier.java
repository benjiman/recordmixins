package typeref;

import java.util.function.Supplier;

public interface NewableSupplier<T> extends Supplier<T>, Newable<T> {}
