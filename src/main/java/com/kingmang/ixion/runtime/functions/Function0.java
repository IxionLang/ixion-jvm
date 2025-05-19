package com.kingmang.ixion.runtime.functions;

@FunctionalInterface
public interface Function0<R> extends Function<R> {
    R invoke();
}
