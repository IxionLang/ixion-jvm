package com.kingmang.ixion.typechecker;

import com.kingmang.ixion.runtime.*;
import com.kingmang.ixion.runtime.CollectionUtil;
import org.apache.commons.lang3.StringUtils;

public class TypeResolver {

    public static Object getValueFromString(String value, BuiltInType t) {
        Object result;
        switch (t) {
            case BOOLEAN -> result = Boolean.valueOf(value);
            case INT -> result = Integer.valueOf(value);
            case FLOAT -> result = Float.valueOf(value);
            case DOUBLE -> result = Double.valueOf(value);
            case STRING -> {
                value = StringUtils.removeStart(value, "\"");
                value = StringUtils.removeEnd(value, "\"");
                result = value;
            }
            default -> throw new AssertionError("Objects not yet implemented!");
        }
        return result;
    }

    public static boolean typesMatch(IxType par, IxType arg) {
        // union
        if (par instanceof UnionType uPar && arg instanceof UnionType uArg) {
            return uPar.types.containsAll(uArg.types) && uArg.types.containsAll(uPar.types);
        }
        if (par instanceof UnionType ua && ua.types.contains(arg)) return true;
        if (arg instanceof UnionType ua && ua.types.contains(par)) return true;

        // any
        var parTypeClass = par.getTypeClass();
        var argTypeClass = arg.getTypeClass();
        if ((parTypeClass != null && parTypeClass.equals(Object.class)) ||
                (argTypeClass != null && argTypeClass.equals(Object.class))) {
            return true;
        }

        // builtin
        if (par instanceof BuiltInType bta && arg instanceof BuiltInType btb) {
            return bta.equals(btb);
        }

        // list
        if (isList(par) && isList(arg)) {
            IxType parContentType = getContentType(par);
            IxType argContentType = getContentType(arg);

            if (parContentType == BuiltInType.ANY || argContentType == BuiltInType.ANY) {
                return true;
            }

            return typesMatch(parContentType, argContentType);
        }

        // struct
        if (arg instanceof StructType ast && par instanceof StructType pst) {
            return ast.parameters.equals(pst.parameters) && ast.name.equals(pst.name);
        }

        // external
        if (par instanceof ExternalType && arg instanceof ExternalType) {
            return par.getName().equals(arg.getName());
        }


        if (isList(arg) && par instanceof ExternalType extPar) {
            return extPar.getTypeClass() == CollectionUtil.IxListWrapper.class;
        }
        if (isList(par) && arg instanceof ExternalType extArg) {
            return extArg.getTypeClass() == CollectionUtil.IxListWrapper.class;
        }

        return false;
    }

    private static boolean isList(IxType type) {
        return type.getName().equals("java.util.List") || type instanceof ListType;
    }

    private static IxType getContentType(IxType type) {
        if (type instanceof ListType listType) {
            return listType.contentType();
        }
        return BuiltInType.ANY;
    }

    public static boolean isNumericCompatible(IxType from, IxType to) {
        if (!(from instanceof BuiltInType fromType) || !(to instanceof BuiltInType toType)) {
            return false;
        }

        return switch (fromType) {
            case INT -> toType == BuiltInType.INT || toType == BuiltInType.FLOAT || toType == BuiltInType.DOUBLE;
            case FLOAT -> toType == BuiltInType.FLOAT || toType == BuiltInType.DOUBLE;
            case DOUBLE -> toType == BuiltInType.DOUBLE;
            default -> false;
        };
    }
}