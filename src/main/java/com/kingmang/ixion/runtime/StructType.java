package com.kingmang.ixion.runtime;

import org.javatuples.Pair;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class StructType implements IxType, Serializable {
    public final List<Pair<String, IxType>> parameters;
    public final List<String> generics;
    public String name;
    public String qualifiedName;
    public String parentName;

    public StructType(String name, List<Pair<String, IxType>> identifiers, List<String> generics) {
        this.name = name;
        this.parameters = identifiers;
        this.generics = generics;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof StructType o) {
            return this.name.equals(o.name);
        } else {
            return false;
        }
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getDescriptor() {
        String n = qualifiedName.replace(":", "/");
        return "L" + n + ";";
    }

    @Override
    public String getInternalName() {
        return getName().replace(".", "/");
    }

    @Override
    public int getLoadVariableOpcode() {
        return Opcodes.ALOAD;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getReturnOpcode() {
        return Opcodes.ARETURN;
    }

    @Override
    public Class<?> getTypeClass() {
        return null;
    }

    public boolean hasGenerics() {
        return !generics.isEmpty();
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public String kind() {
        return "struct";
    }

    @Override
    public String toString() {
        return "type " + getName() + "= struct" + " {" + parameters.stream().map(m -> m.getValue1().getName()).collect(Collectors.joining(", ")) + "}";
    }

}
