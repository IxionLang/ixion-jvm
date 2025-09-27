package com.kingmang.ixion.runtime;

import org.objectweb.asm.Opcodes;

import java.util.Set;
import java.util.stream.Collectors;

public class UnionType implements IxType {
    public Set<IxType> types;

    public UnionType(Set<IxType> types) {
        this.types = types;

    }

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
        return null;
    }

    @Override
    public int getLoadVariableOpcode() {
        return Opcodes.ALOAD;
    }

    @Override
    public String getName() {
        return types.stream().map(IxType::getName).collect(Collectors.joining(" | "));
    }


    @Override
    public int getReturnOpcode() {
        return Opcodes.ARETURN;
    }


    @Override
    public Class<?> getTypeClass() {
        return null;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public String kind() {
        return null;
    }


    @Override
    public String toString() {
        return types.stream().map(IxType::getName).collect(Collectors.joining(" | "));
    }
}
