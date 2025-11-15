package com.kingmang.ixion.runtime.ixfunction;

@FunctionalInterface
public interface IxFunction1<A, R> {
    R apply(A a);
}