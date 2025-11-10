package com.kingmang.ixion.exception;

public class CannotPostfixException extends IxException {
        public CannotPostfixException() {
            super(3, "Operator `{0}` cannot be applied to expression of type `{1}`.", null);
        }
}