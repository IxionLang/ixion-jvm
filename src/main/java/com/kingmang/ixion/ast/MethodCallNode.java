package com.kingmang.ixion.ast;

import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Unthrow;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodCallNode implements Node {

	private final Node left;
	private final Token name;
	private final List<Node> args;
	private boolean isStaticAccess;
	private final boolean isSuper;

	public MethodCallNode(Node left, Token name, List<Node> args, boolean isSuper) {
		this.left = left;
		this.name = name;
		this.args = args;
		this.isStaticAccess = false;
		this.isSuper = isSuper;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		getLeftType(context.getContext());
		left.visit(context);

		IxType returnType = left.getReturnType(context.getContext());
		if(returnType.isNullable()) {
			throw new IxException(name, "Cannot use '.' to call methods on a nullable type ('%s')".formatted(returnType));
		}

		visitCall(context);
	}

	public void visitCall(FileContext context) throws IxException {
		IxType leftType = getLeftType(context.getContext());

		if(leftType.isArray() && name.value().equals("length") && args.size() == 0) {
			context.getContext().getMethodVisitor().visitInsn(Opcodes.ARRAYLENGTH);
			return;
		}

		if(!leftType.isObject()) {
			throw new IxException(name, "Cannot invoke method on type '%s'".formatted(leftType));
		}

		Method toCall = resolve(leftType, context.getContext());

		IxType[] resolvedTypes = IxType.getType(toCall).getArgumentTypes();

		for(int i = 0; i < args.size(); i++) {
			Node arg = args.get(i);
			IxType resolvedType = resolvedTypes[i];

			if (arg instanceof LambdaDeclarationNode lambda)
				lambda.setItf(resolvedType);

			arg.visit(context);
			try {
				IxType argType = arg.getReturnType(context.getContext());
				resolvedType.isAssignableFrom(argType, context.getContext(), true); // DomamaN202: IDK WTF
				argType.autoBox(context.getContext().getMethodVisitor());
			} catch (ClassNotFoundException e) {
				throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
		}

		TypeUtil.insertInvoke(context, toCall, isSuper);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType leftType = getLeftType(context);

		if(leftType.isArray() && name.value().equals("length") && args.size() == 0) return IxType.INT_TYPE;

		return IxType.getType(resolve(leftType, context)).getReturnType();
	}

	private IxType getLeftType(Context context) throws IxException {
		if(left instanceof VariableAccessNode) {
			VariableAccessNode van = (VariableAccessNode) left;
			van.setMemberAccess(true);
			IxType leftType = left.getReturnType(context);
			isStaticAccess = van.isStaticClassAccess();
			return leftType;
		}
		return left.getReturnType(context);
	}

	private Method resolve(IxType leftType, Context context) throws IxException {
		IxType[] argTypes = args.stream()
				.map(n -> Unthrow.wrap(() -> n.getReturnType(context))).toArray(IxType[]::new);

		Class<?> klass;

		try {
			klass = Class.forName(leftType.getClassName(), false, context.getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		ArrayList<Pair<Integer, Method>> possible = new ArrayList<>();

		try {
			out:
			for (Method method : klass.getMethods()) {
				if(!method.getName().equals(name.value())) continue;
				IxType[] expectArgs = IxType.getType(method).getArgumentTypes();

				if (expectArgs.length != argTypes.length) continue;

				int changes = 0;

				for (int i = 0; i < expectArgs.length; i++) {
					IxType expectArg = expectArgs[i];
					IxType arg = argTypes[i];

					if (arg.equals(IxType.VOID_TYPE))
						continue out;

					if (expectArg.isAssignableFrom(arg, context, false)) {
						if (!expectArg.equals(arg))
							changes += expectArg.assignChangesFrom(arg);
					} else if (expectArg.isPrimitive() != arg.isPrimitive()) {
						changes += 3;
					} else if (expectArg.isPrimitive() ^ arg.isPrimitive()) {
						if (!arg.toPrimitiveClass(context).equals(expectArg.toPrimitiveClass(context)))
							continue out;
					} else if (expectArg.toClass(context).isAnnotationPresent(FunctionalInterface.class) && args.get(i) instanceof LambdaDeclarationNode) {
						changes++;
					} else {
						continue out;
					}
				}

				possible.add(new Pair<>(changes, method));
			}
		}
		catch(ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		if(possible.isEmpty()) {
			throw new IxException(name,
					"Could not resolve method '%s' with arguments: %s".formatted(name.value(),
							argTypes.length == 0 ? "(none)" :
									Stream.of(argTypes).map(IxType::toString).collect(Collectors.joining(", "))));
		}

		List<Pair<Integer, Method>> appliedPossible = possible.stream()
				.filter(p -> Modifier.isStatic(p.second().getModifiers()) == isStaticAccess)
				.sorted(Comparator.comparingInt(Pair::first)).toList();

		if(appliedPossible.isEmpty()) {
			if(isStaticAccess)
				throw new IxException(name, "Cannot invoke non-static method from static class.");
			else
				throw new IxException(name, "Cannot invoke static method from non-static object.");
		}

		return appliedPossible.get(0).second();
	}

	@Override
	public String toString() {
		return "%s.%s(%s)".formatted(left, name.value(), args.stream().map(Node::toString).collect(Collectors.joining(", ")));
	}
}
