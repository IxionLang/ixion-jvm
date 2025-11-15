package com.kingmang.ixion.runtime.ixfunction;

@FunctionalInterface
public interface IxFunction3<A, B, C, R> {
    R apply(A a, B b, C c);
}