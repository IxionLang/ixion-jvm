package com.kingmang.ixion.exception;

public class RedeclarationException extends IxException {
    public RedeclarationException() {
        super(18, "Redeclaration of variable `{0}`.", "You cannot redeclare variables.");
    }
}