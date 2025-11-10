package com.kingmang.ixion.runtime;

import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.typechecker.TypeUtils;
import org.javatuples.Pair;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefType extends StructType {
    public final Map<String, Integer> localMap = new HashMap<>();
    public final Map<String, Integer> argMap = new HashMap<>();
    public String name;
    public IxType returnType = BuiltInType.VOID;
    public GeneratorAdapter ga = null;
    public boolean glue = false;
    public boolean hasReturn2 = false;
    public boolean isPrefixed = false;
    public String owner;

    public List<Map<String, IxType>> specializations = new ArrayList<>();

    public Map<String, IxType> currentSpecialization = null;
    public IxFile external;

    public DefType(String name, List<Pair<String, IxType>> parameters) {
        super(name, parameters, new ArrayList<>());
        this.name = name;

    }

    public DefType(String name, List<Pair<String, IxType>> parameters, List<String> generics) {
        super(name, parameters, generics);
        this.name = name;

    }

    public static IxType getSpecializedType(Map<String, IxType> specialization, String key) {
        return specialization.get(key);
    }

    public List<Pair<String, IxType>> buildParametersFromSpecialization(Map<String, IxType> specialization) {
        var p = new ArrayList<Pair<String, IxType>>();
        for (Pair<String, IxType> pair : parameters) {
            var pt = pair.getValue1();
            if (pt instanceof GenericType gt) {
                p.add(pair.setAt1(specialization.get(gt.key())));
            } else {
                p.add(pair);
            }
        }
        return p;
    }

    public Map<String, IxType> buildSpecialization(List<Expression> arguments) {
        var argTypes = arguments.stream().map(ex -> ex.realType).toList();
        var specialization = new HashMap<String, IxType>();
        for (int i = 0; i < parameters.size(); i++) {
            var p = parameters.get(i);
            var pt = p.getValue1();
            if (pt instanceof GenericType gt) {
                specialization.put(gt.key(), argTypes.get(i));
            }
        }
        return specialization;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getDescriptor() {
        return null;
    }

    @Override
    public String getInternalName() {
        return null;
    }

    @Override
    public int getLoadVariableOpcode() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getReturnOpcode() {
        return 0;
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
        return "function";
    }

    @Override
    public String toString() {
        return "def " + name + "(" + TypeUtils.parameterString(parameters) + ") " + returnType;
    }
}
