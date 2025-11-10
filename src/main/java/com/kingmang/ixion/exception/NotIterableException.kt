package com.kingmang.ixion.exception;

public class NotIterableException extends IxException {
    public NotIterableException() {
        super(15, "Expression of type `{0}` is not iterable.",
                "Check to be sure the type is iterable, like a list or range.");
    }
}