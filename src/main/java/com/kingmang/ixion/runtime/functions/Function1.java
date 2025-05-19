package com.kingmang.ixion.runtime.functions;

@FunctionalInterface
public interface Function1<P1, R> extends Function<R> {
    R invoke(P1 p1);
}
