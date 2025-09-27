package com.kingmang.ixion.exception;

public class ListLiteralIncompleteException extends IxException {
    public ListLiteralIncompleteException() {
        super(9, "List literals must have one or more elements.", "To create an empty list do `type[]`.");
    }
}