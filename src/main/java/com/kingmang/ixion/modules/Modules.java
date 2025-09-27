package com.kingmang.ixion.modules;

import com.kingmang.ixion.runtime.BuiltInType;
import com.kingmang.ixion.runtime.DefType;
import com.kingmang.ixion.runtime.ExternalType;
import com.kingmang.ixion.runtime.IxType;
import com.kingmang.ixion.typechecker.TypeUtils;
import org.javatuples.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Modules {

    public static final Map<String, Class<?>> modules = new HashMap<>();
    static {
        modules.put("prelude", Prelude.class);
        modules.put("gui", Prelude.class);
        modules.put("http", HttpModule.class);
    }

    public static List<DefType> getExports(String module) {
        var result = new ArrayList<DefType>();
        if (modules.containsKey(module)) {
            var c = modules.get(module);
            var m = c.getMethods();
            for (var method : m) {
                String name = method.getName();
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue;
                }
                var parameters = getPairs(method);
                boolean isPrefixed = false;
                if (name.startsWith("_")) {
                    name = name.substring(1);
                    isPrefixed = true;
                }
                var funcType = new DefType(name, parameters);
                funcType.isPrefixed = isPrefixed;
                funcType.returnType = new ExternalType(method.getReturnType());

                var bt = TypeUtils.getFromString(method.getReturnType().getName());
                if (bt != null) funcType.returnType = bt;

                funcType.glue = true;
                funcType.owner = c.getName().replace('.', '/');
                result.add(funcType);

            }

        }
        return result;
    }

    private static ArrayList<Pair<String, IxType>> getPairs(Method method) {
        var parameters = new ArrayList<Pair<String, IxType>>();
        for (var p : method.getParameterTypes()) {

            IxType t = switch (p.getName()) {
                case "int" -> BuiltInType.INT;
                case "float" -> BuiltInType.FLOAT;
                case "boolean" -> BuiltInType.BOOLEAN;
                default -> new ExternalType(p);
            };

            var id = Pair.with("_", t);
            parameters.add(id);
        }
        return parameters;
    }


}