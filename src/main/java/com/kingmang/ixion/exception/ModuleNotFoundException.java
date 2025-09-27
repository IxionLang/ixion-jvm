package com.kingmang.ixion.exception;

public class ModuleNotFoundException extends IxException {
    public ModuleNotFoundException() {
        super(13, "Module `{0}` is not found.", "Is the module misspelled?");
    }
}