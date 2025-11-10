package com.kingmang.ixion.exception;

public class IdentifierNotFoundException extends IxException {
    public IdentifierNotFoundException() {
        super(7, "Identifier `{0}` not found.",
                "Make sure that all identifiers are defined, builtin or imported.");
    }
}