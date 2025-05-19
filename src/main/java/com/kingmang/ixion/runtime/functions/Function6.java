package com.kingmang.ixion.runtime.functions;

@FunctionalInterface
public interface Function6<P1, P2, P3, P4, P5, P6, R> extends Function<R> {
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
}
