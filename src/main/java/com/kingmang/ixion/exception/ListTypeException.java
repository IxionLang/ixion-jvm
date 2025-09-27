package com.kingmang.ixion.exception;

public class ListTypeException extends IxException {
    public ListTypeException() {
        super(10, "Expression of type `{0}` cannot be added to list of inferred type `{1}`",
                "Lists may only contain elements of the same type.");
    }
}