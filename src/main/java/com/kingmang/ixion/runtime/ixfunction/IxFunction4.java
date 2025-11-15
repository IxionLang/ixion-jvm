package com.kingmang.ixion.runtime.ixfunction;

@FunctionalInterface
public interface IxFunction4<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}