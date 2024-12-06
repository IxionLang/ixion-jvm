package com.kingmang.ixion.monad;

public interface Monad<T> {
    T get();

    T getValue();

    default boolean isPresent() {return false;}

    default boolean isError() {return false;}

}
