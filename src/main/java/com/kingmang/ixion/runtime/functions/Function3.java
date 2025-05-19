package com.kingmang.ixion.runtime.functions;

@FunctionalInterface
public interface Function3<P1, P2, P3, R> extends Function<R> {
    R invoke(P1 p1, P2 p2, P3 p3);
}
