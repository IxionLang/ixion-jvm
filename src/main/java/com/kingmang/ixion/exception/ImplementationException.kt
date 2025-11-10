package com.kingmang.ixion.exception;

public class ImplementationException extends IxException {
    public ImplementationException() {
        super(8, "{0}", "If you are seeing this message it indicates a regression in the Imp compiler. Please contact the developers.");
    }
}