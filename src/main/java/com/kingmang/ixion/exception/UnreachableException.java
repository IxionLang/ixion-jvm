package com.kingmang.ixion.exception;

public class UnreachableException extends IxException {
    public UnreachableException() {
        super(22, "Unreachable code detected.", null);
    }
}