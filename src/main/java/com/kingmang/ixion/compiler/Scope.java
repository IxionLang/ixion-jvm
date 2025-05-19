package com.kingmang.ixion.compiler;

import com.kingmang.ixion.ast.LambdaDeclarationNode;
import com.kingmang.ixion.compiler.ix_function.types.TypesFunction;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.output.PrintFunction;
import com.kingmang.ixion.compiler.ix_function.output.PrintlnFunction;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;


public class Scope {
	private FileContext fileContext;
	private Context context;
	private HashMap<String, ArrayList<IxFunction>> functionMap;
	private HashMap<String, Variable> variables;
	@Setter
    private int localIndex;
	@Getter
    private IxType returnType;
	@Getter
    private boolean returned;

	public Scope(FileContext context) {
		functionMap = new HashMap<>();
		variables = new HashMap<>();
		this.context = context.getContext();
		this.localIndex = 0;
		this.returnType = IxType.VOID_TYPE;
        addLangFunctions();
		addPrintlnFunctions();
		updateCurrentClassMethods(context);
	}

	public Scope(Context context) {
		this.context = context;
		this.functionMap = new HashMap<>();
		this.variables = new HashMap<>();
        addLangFunctions();
		addPrintlnFunctions();
	}

    private void addLangFunctions(){
        addFunction(TypesFunction.INT_BOXED);
        addFunction(TypesFunction.FLOAT_BOXED);
		addFunction(TypesFunction.AS_STRING_CHAR);
		addFunction(TypesFunction.TOINT);
		addFunction(TypesFunction.TOFLOAT);
    }


	private void addPrintlnFunctions() {
		addFunction(PrintlnFunction.PRINTLN_VOID);
		addFunction(PrintlnFunction.PRINTLN_DOUBLE);
		addFunction(PrintlnFunction.PRINTLN_FLOAT);
		addFunction(PrintlnFunction.PRINTLN_INTEGER);
		addFunction(PrintlnFunction.PRINTLN_BOOLEAN);
		addFunction(PrintlnFunction.PRINTLN_CHAR);
		addFunction(PrintlnFunction.PRINTLN_LONG);
		addFunction(PrintlnFunction.PRINTLN_OBJECT);


		addFunction(PrintFunction.PRINT_VOID);
		addFunction(PrintFunction.PRINT_DOUBLE);
		addFunction(PrintFunction.PRINT_FLOAT);
		addFunction(PrintFunction.PRINT_INTEGER);
		addFunction(PrintFunction.PRINT_BOOLEAN);
		addFunction(PrintFunction.PRINT_CHAR);
		addFunction(PrintFunction.PRINT_LONG);
		addFunction(PrintFunction.PRINT_OBJECT);
	}

	private Scope() {}

	public void updateCurrentClassMethods(FileContext context) {
		Class<?> klass = context.getCurrentClass();
		if(klass == null) return;
		for(Method m : klass.getDeclaredMethods()) {
			int modifier = m.getModifiers();
			boolean isStatic = Modifier.isStatic(modifier);
			addFunction(new IxFunction(isStatic ? IxFunctionType.STATIC : IxFunctionType.CLASS, m.getName(), Type.getInternalName(klass), IxType.getType(m)));
		}

		for(Field f : klass.getDeclaredFields()) {
			int modifier = f.getModifiers();
			boolean isStatic = Modifier.isStatic(modifier);
			addVariable(new Variable(isStatic ? VariableType.STATIC : VariableType.CLASS, f.getName(), Type.getInternalName(klass), IxType.getType(f.getType()), Modifier.isFinal(f.getModifiers())));
		}
	}

	public Scope nextDepth() {
		Scope scope = new Scope();
		scope.setFileContext(fileContext)
				.setContext(context)
				.setVariables(new HashMap<>(variables))
				.setFunctionMap(new HashMap<>(functionMap))
				.setReturnType(returnType)
				.setReturned(returned)
				.setLocalIndex(localIndex);
		return scope;
	}

	public ArrayList<IxFunction> lookupFunctions(String name) {
		return functionMap.get(name);
	}

	public IxFunction lookupFunction(String name, IxType[] argsTypes, Node[] args) throws ClassNotFoundException {
		ArrayList<IxFunction> funcs = functionMap.get(name);
		if(funcs == null) return null;

		ArrayList<Pair<Integer, IxFunction>> possible = new ArrayList<>();

		out : for(IxFunction f : funcs) {
			IxType[] expectArgs = f.type().getArgumentTypes();

			if (expectArgs.length != argsTypes.length) continue;

			int changes = 0;

			for (int i = 0; i < expectArgs.length; i++) {
				IxType expectArg = expectArgs[i];
				IxType arg = argsTypes[i];

				if (arg.equals(IxType.VOID_TYPE))
					continue out;

				if (expectArg.isAssignableFrom(arg, context, false)) {
					if (!expectArg.equals(arg)) changes += expectArg.assignChangesFrom(arg);
				} else if (expectArg.toClass(context).isAnnotationPresent(FunctionalInterface.class) && args[i] instanceof LambdaDeclarationNode) {
					changes++;
				} else {
					continue out;
				}
			}
			possible.add(new Pair<>(changes, f));
		}
		if(possible.isEmpty()) return null;

		possible.sort(Comparator.comparingInt(Pair::first));

		return possible.get(0).second();
	}

	public IxFunction lookupFunction(String name, IxType[] argsTypes, Node[] args, boolean visit, FileContext fc) throws ClassNotFoundException, IxException {
		ArrayList<IxFunction> funcs = functionMap.get(name);
		if(funcs == null) return null;

		ArrayList<Pair<Integer, IxFunction>> possible = new ArrayList<>();

		out : for(IxFunction f : funcs) {
			IxType[] expectArgs = f.type().getArgumentTypes();

			if (expectArgs.length != argsTypes.length) continue;

			int changes = 0;

			for (int i = 0; i < expectArgs.length; i++) {
				IxType expectArg = expectArgs[i];
				IxType arg = argsTypes[i];

				if (arg.equals(IxType.VOID_TYPE))
					continue out;

				if (expectArg.isAssignableFrom(arg, context, false)) {
					if (!expectArg.equals(arg)) changes += expectArg.assignChangesFrom(arg);
				} else if (expectArg.toClass(context).isAnnotationPresent(FunctionalInterface.class) && args[i] instanceof LambdaDeclarationNode) {
					changes++;
				} else {
					continue out;
				}
			}
			possible.add(new Pair<>(changes, f));
		}
		if(possible.isEmpty()) return null;

		possible.sort(Comparator.comparingInt(Pair::first));

		IxFunction resolved = possible.get(0).second();

		IxType[] resolvedArgs = resolved.type().getArgumentTypes();

		if(visit) {
			for (int i = 0; i < resolvedArgs.length; i++) {
				IxType resolvedArg = resolvedArgs[i];
				Node arg = args[i];

				if (arg instanceof LambdaDeclarationNode lambda)
					lambda.setItf(resolvedArg);

				arg.visit(fc);
				resolvedArg.isAssignableFrom(argsTypes[i], context, true);
			}
		}

		return resolved;
	}

	public IxFunction exactLookupFunction(String name, IxType... args) throws ClassNotFoundException {
		ArrayList<IxFunction> funcs = functionMap.get(name);
		if(funcs == null) return null;

		out : for(IxFunction f : funcs) {
			IxType[] expectArgs = f.type().getArgumentTypes();

			if(expectArgs.length != args.length) continue;

			for(int i = 0; i < expectArgs.length; i++) {
				IxType expectArg = expectArgs[i];
				IxType arg = args[i];

				if(arg.equals(IxType.VOID_TYPE))
					continue out;

				if(!expectArg.toClass(context).isAssignableFrom(arg.toClass(context)))
					continue out;
			}
			return f;
		}
		return null;
	}

	public IxFunction lookupFunction(String name, IxType... args) throws ClassNotFoundException {
		try {
			return lookupFunction(name, args, null, false, null);
		} catch (IxException e) {
			assert false;
		}
		return null;
	}

	public void addFunction(IxFunction function) {
		String name = function.name();
		functionMap.computeIfAbsent(name, k -> new ArrayList<>());

		functionMap.get(name).add(function);
	}

	public void addVariable(Variable variable) {
		String name = variable.getName();
		variables.put(name, variable);
	}

	public Variable lookupVariable(String name) {
		return variables.get(name);
	}

	public int nextLocal() {
		return localIndex++;
	}

    private Scope setFileContext(FileContext fileContext) {
		this.fileContext = fileContext;
		return this;
	}

	private Scope setContext(Context context) {
		this.context = context;
		return this;
	}

	private Scope setFunctionMap(HashMap<String, ArrayList<IxFunction>> functionMap) {
		this.functionMap = functionMap;
		return this;
	}

	private Scope setVariables(HashMap<String, Variable> variables) {
		this.variables = variables;
		return this;
	}

    public Scope setReturnType(IxType returnType) {
		this.returnType = returnType;
		return this;
	}

    public Scope setReturned(boolean returned) {
		this.returned = returned;
		return this;
	}
}
