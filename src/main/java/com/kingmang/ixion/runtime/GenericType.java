package com.kingmang.ixion.runtime;

public record GenericType(String key) implements IxType {


    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getDescriptor() {
        return "Ljava/lang/Object;";
    }

    @Override
    public String getInternalName() {
        return key;
    }

    @Override
    public int getLoadVariableOpcode() {
        throw new IllegalCallerException("Don't use ALOAD on Generic Types directly. Specialization required.");
    }


    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getReturnOpcode() {
        throw new IllegalCallerException("Don't use ARETURN on Generic Types directly. Specialization required.");
    }

    @Override
    public Class<?> getTypeClass() {
        return Object.class;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public String kind() {
        return null;
    }
}
