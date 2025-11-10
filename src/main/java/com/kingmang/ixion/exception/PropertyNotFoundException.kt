package com.kingmang.ixion.exception;

public class PropertyNotFoundException extends IxException {
    public PropertyNotFoundException() {
        super(17, "Type `{0}` contains no field `{1}`.", null);
    }
}