package com.kingmang.ixion.exception;

public class FieldNotPresentException extends IxException {
    public FieldNotPresentException() {
            super(5, "Field `{0}` not present on type `{1}`.", null);
    }
}