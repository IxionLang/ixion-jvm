package com.kingmang.ixion.exception;

public class MutabilityException extends IxException {
    public MutabilityException() {
        super(14, "Variable `{0}` is immutable and cannot receive assignment.",
                "Declare a variable with the `mut` keyword to allow mutability.");
    }
}