package com.kingmang.ixion.exception;

public class VoidUsageException extends IxException {
    public VoidUsageException() {
        super(23, "Cannot use the result of a void method in an expression.",
                "Methods returning `void` don't have any result so there is no return type to use.");
    }
}