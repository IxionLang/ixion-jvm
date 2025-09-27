package com.kingmang.ixion.exception;

public class CannotApplyOperatorException extends IxException {
    public CannotApplyOperatorException() {
        super(2, "Operator `{0}` cannot be applied to types `{1}` and `{2}`", null);
    }
}