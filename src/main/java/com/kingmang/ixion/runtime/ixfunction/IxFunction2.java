package com.kingmang.ixion.runtime.ixfunction;

@FunctionalInterface
public interface IxFunction2<A, B, R> {
    R apply(A a, B b);
}