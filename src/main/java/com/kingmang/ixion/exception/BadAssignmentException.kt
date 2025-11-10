package com.kingmang.ixion.exception;

public class BadAssignmentException extends IxException {
    public BadAssignmentException() {
        super(1, "Variable `{0}` of type `{1}` cannot accept assignment of type `{2}`.",
                    "Check that both sides of the assignment have the same type.");
    }
}