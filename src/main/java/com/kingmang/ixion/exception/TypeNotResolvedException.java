package com.kingmang.ixion.exception;

public class TypeNotResolvedException extends IxException {
    public TypeNotResolvedException() {
        super(21, "Variable `{0}` cannot be resolved to a type.", "Make sure all variables are properly spelled etc.");
    }
}