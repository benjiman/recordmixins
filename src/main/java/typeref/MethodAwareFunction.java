package typeref;

import java.util.function.Function;

public interface MethodAwareFunction<T,R> extends Function<T,R>, MethodFinder { }