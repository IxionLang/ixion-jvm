package com.kingmang.ixion.typechecker;

import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.runtime.BuiltInType;
import com.kingmang.ixion.runtime.IxType;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TypeUtils {
    public static String parameterString(List<Pair<String, IxType>> parameters) {
        return parameters.stream().map(p -> p.getValue0() + " " + p.getValue1()).collect(Collectors.joining(", "));
    }

    public static Class<?> convert(Class<?> c) {
        if (c == Integer.class) return int.class;
        if (c == Float.class) return float.class;
        if (c == Boolean.class) return boolean.class;

        return null;
    }

    public static String getMethodDescriptor(Collection<Pair<String, IxType>> parameters, IxType returnType) {
        String parametersDescriptor = parameters.stream()
                .map(parameter -> parameter.getValue1().getDescriptor())
                .collect(Collectors.joining("", "(", ")"));
        String returnDescriptor = returnType.getDescriptor();
        return parametersDescriptor + returnDescriptor;
    }


    public static BuiltInType getFromToken(TokenType tokenType) {
        return switch (tokenType) {
            case TRUE, FALSE -> BuiltInType.BOOLEAN;
            case STRING -> BuiltInType.STRING;
            case INT -> BuiltInType.INT;
            case FLOAT -> BuiltInType.FLOAT;
            case DOUBLE -> BuiltInType.DOUBLE;
            default -> throw new IllegalStateException("Unexpected value: " + tokenType);
        };
    }

    public static BuiltInType getFromString(String value) {
        return switch (value) {
            case "int" -> BuiltInType.INT;
            case "float" -> BuiltInType.FLOAT;
            case "double" -> BuiltInType.DOUBLE;
            case "bool" -> BuiltInType.BOOLEAN;
            case "string" -> BuiltInType.STRING;
            case "any" -> BuiltInType.ANY;
            case "void" -> BuiltInType.VOID;
            default -> null;
        };
    }
}
