package com.kingmang.ixion.exception;

public class ParameterTypeMismatchException extends IxException {
    public ParameterTypeMismatchException() {
        super(16, "Argument of type `{0}` cannot supply a parameter of type `{1}`.", null);
    }
}