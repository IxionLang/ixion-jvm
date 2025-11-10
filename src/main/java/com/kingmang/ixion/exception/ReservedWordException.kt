package com.kingmang.ixion.exception;

public class ReservedWordException extends IxException {
    public ReservedWordException() {
        super(19, "Identifier `{0}` is a reserved word.", null);
    }
}