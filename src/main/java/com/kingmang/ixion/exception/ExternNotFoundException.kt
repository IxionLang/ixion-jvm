package com.kingmang.ixion.exception;

public class ExternNotFoundException extends IxException {
    public ExternNotFoundException() {
        super(4, "External object `{0}` not found.", "Ensure the external type you are referencing actually exists.");
    }
}