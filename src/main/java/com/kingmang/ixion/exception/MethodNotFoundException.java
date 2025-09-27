package com.kingmang.ixion.exception;

public class MethodNotFoundException extends IxException {
    public MethodNotFoundException() {
        super(12, "Method `{0}` not found.", null);
    }
}