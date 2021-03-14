package typeref;

import java.util.function.Predicate;

public interface MethodAwarePredicate<T> extends Predicate<T>, MethodFinder { }