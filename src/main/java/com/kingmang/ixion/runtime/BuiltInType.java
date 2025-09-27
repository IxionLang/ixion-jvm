package com.kingmang.ixion.runtime;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum BuiltInType implements IxType, Serializable {
    BOOLEAN(
            "bool",
            Boolean.class,
            "Z",
            TypeSpecificOpcodes.INT,
            false,
            false
    ),
    INT(
            "int",
            Integer.class,
            "I",
            TypeSpecificOpcodes.INT,
            0,
            true
    ),

    FLOAT(
            "float",
            Float.class,
            "F",
            TypeSpecificOpcodes.FLOAT,
            0.0f,
            true
    ),

    DOUBLE(
            "double",
            Double.class,
            "D",
            TypeSpecificOpcodes.DOUBLE,
            0.0d,
            true
    ),

    STRING(
            "string",
            String.class,
            "Ljava/lang/String;",
            TypeSpecificOpcodes.OBJECT,
            "",
            false
    ),

    VOID(
            "void",
            void.class,
            "V",
            TypeSpecificOpcodes.VOID,
            null,
            false
    ),

    ANY(
            "any",
            Object.class,
            "Ljava/lang/Object;",
            TypeSpecificOpcodes.OBJECT,
            false,
            false
    );

    public final static Map<BuiltInType, Integer> widenings = new HashMap<>();

    static {
        widenings.put(BOOLEAN, -1);
        widenings.put(INT, 0);
        widenings.put(FLOAT, 1);
        widenings.put(DOUBLE, 2);
        widenings.put(STRING, 10);
    }

    private final String name;
    private final Class<?> typeClass;
    private final String descriptor;
    private final TypeSpecificOpcodes opcodes;
    private final Object defaultValue;
    private final boolean isNumeric;

    BuiltInType(String name, Class<?> typeClass, String descriptor, TypeSpecificOpcodes opcodes, Object defaultValue, boolean isNumeric) {
        this.name = name;
        this.typeClass = typeClass;
        this.descriptor = descriptor;
        this.opcodes = opcodes;
        this.defaultValue = defaultValue;
        this.isNumeric = isNumeric;

    }

    public static BuiltInType widen(BuiltInType a, BuiltInType b) {
        Integer priorityA = widenings.get(a);
        Integer priorityB = widenings.get(b);

        if (priorityA == null && priorityB == null) {
            return a;
        }
        if (priorityA == null) {
            return b;
        }
        if (priorityB == null) {
            return a;
        }

        if (priorityA > priorityB) {
            return a;
        }
        return b;
    }

    public void doBoxing(MethodVisitor mv) {
        switch (this) {
            case INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case STRING:
                break;
            case ANY:
                break;
            default:
                System.err.println("Boxing isn't supported for that type.");
                break;
        }
    }

    public void doUnboxing(MethodVisitor mv) {
        switch (this) {
            case INT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            }
            case FLOAT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "floatValue", "()F", false);
            }
            case DOUBLE -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            }
            case BOOLEAN -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            }
            case STRING -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            default -> {
                System.err.println("Unboxing isn't supported for that type.");
                System.exit(29);
            }
        }
    }

    public int getAddOpcode() {
        return opcodes.getAdd();
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    public int getDivideOpcode() {
        return opcodes.getDivide();
    }

    @Override
    public String getInternalName() {
        return getDescriptor();
    }

    @Override
    public int getLoadVariableOpcode() {
        return opcodes.getLoad();
    }

    public int getMultiplyOpcode() {
        return opcodes.getMultiply();
    }

    @Override
    public String getName() {
        return name;
    }

    public int getNegOpcode() {
        return opcodes.getNeg();
    }

    @Override
    public int getReturnOpcode() {
        return opcodes.getReturn();
    }

    public int getSubtractOpcode() {
        return opcodes.getSubtract();
    }

    @Override
    public Class<?> getTypeClass() {
        return typeClass;
    }

    @Override
    public boolean isNumeric() {
        return isNumeric;
    }

    @Override
    public String kind() {
        return null;
    }

    public void pushOne(GeneratorAdapter ga) {
        switch (this) {
            case INT -> ga.push(1);
            case FLOAT -> ga.push(1.0f);
            case DOUBLE -> ga.push(1.0);
            default -> ga.push(0);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public void unboxNoCheck(MethodVisitor mv) {
        switch (this) {
            case INT -> mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            case FLOAT -> mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
            case DOUBLE -> mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            case BOOLEAN -> mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            default -> {
            }
        }
    }
}
