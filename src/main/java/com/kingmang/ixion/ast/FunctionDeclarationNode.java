package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.*;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.util.Unthrow;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionDeclarationNode implements Node {

	public enum DeclarationType {
		STANDARD,
		EXPRESSION
	}

	private final DeclarationType type;
	private final Token name;
	private final Node body;
	@Getter
	private final List<Pair<Token, Node>> parameters;
	private final Node returnTypeNode;
	private final List<Node> throwsList;
	private final Token access;
	private final Token staticModifier;
	private final List<Token> annotations;
	private IxType returnType;
	private String descriptor;

	public FunctionDeclarationNode(
			DeclarationType type,
			Token name,
			Node body,
			List<Pair<Token, Node>> parameters,
			Node returnType,
			List<Node> throwsList,
			Token access,
			Token staticModifier,
			List<Token> annotations) {

		this.type = type;
		this.name = name;
		this.body = body;
		this.parameters = parameters;
		this.returnTypeNode = returnType;
		this.throwsList = throwsList;
		this.access = access;
		this.staticModifier = staticModifier;
		this.annotations = annotations;
	}

	@Override
	public void preprocess(Context context) throws IxException {
		if(context.getType() == ContextType.GLOBAL) {
			preprocessGlobal(context);
		}
		else {
			preprocessClass(context);
		}

	}

	private void preprocessGlobal(Context context) throws IxException {
		try {
			if (
					context.getScope().exactLookupFunction(name.value(),
							parameters.stream().map(n -> Unthrow.wrap(() ->
							n.second().getReturnType(context)))
							.toArray(IxType[]::new)) != null
			)
				throw new IxException(name,
					"Redefinition of function '%s' in global scope.".formatted(name.value()));

			computeReturnType(context, true);

			ArrayList<IxFunction> functions = context.getScope().lookupFunctions(name.value());
			if(functions != null) {
				IxType expectedReturnType = functions.get(0).type().getReturnType();
				if (!expectedReturnType.equals(returnType)) {
					throw new IxException(name,
							"IxFunction overloads may only differ in parameters, not return type. (%s =/= %s)"
									.formatted(returnType, expectedReturnType));
				}
			}

			makeDescriptor(context);
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		MethodVisitor mv = makeGlobalFunction(context);
		finalizeMethod(mv, context);

		context.getScope().addFunction(new IxFunction(IxFunctionType.STATIC, name.value(), context.getCurrentClass(), IxType.getMethodType(descriptor)));
	}

	private void preprocessClass(Context context) throws IxException {
		try {
			IxFunction function = context.getScope().exactLookupFunction(name.value(), parameters.stream().map(n -> Unthrow.wrap(() -> n.second().getReturnType(context))).toArray(IxType[]::new));

			if(function != null && function.functionType() == IxFunctionType.CLASS) {
				throw new IxException(name, "Redefinition of function '%s' in current class.".formatted(name.value()));
			}

			computeReturnType(context, false);

			ArrayList<IxFunction> functions = context.getScope().lookupFunctions(name.value());
			if(functions != null) {
				IxType expectedReturnType = functions.get(0).type().getReturnType();
				if (!expectedReturnType.equals(returnType)) {
					throw new IxException(name,
							"IxFunction overloads may only differ in parameters, not return type. (%s =/= %s)"
									.formatted(returnType, expectedReturnType));
				}
			}

			makeDescriptor(context);
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		MethodVisitor mv = makeClassFunction(context);
		finalizeMethod(mv, context);

		context.getScope().addFunction(new IxFunction(IxFunctionType.CLASS, name.value(), context.getCurrentClass(), IxType.getMethodType(descriptor)));
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().setConstructor(false);
		MethodVisitor mv;
		if(context.getContext().getType() == ContextType.GLOBAL) mv = makeGlobalFunction(context.getContext());
		else mv = makeClassFunction(context.getContext());
		createMainMethod(context.getContext());
		mv.visitCode();

		for(Token token : annotations) {
			if(token.type() == TokenType.OVERRIDE)
				mv.visitAnnotation("Override", true);
		}

		context.getContext().setMethodVisitor(mv);

		ContextType prev = context.getContext().getType();

		boolean isStatic = isStatic(context.getContext());

		context.getContext().setType(ContextType.FUNCTION);

		Scope outer = context.getContext().getScope();

		context.getContext().setScope(outer.nextDepth());

		context.getContext().getScope().setReturnType(returnType);


		context.getContext().setStaticMethod(isStatic);

		addParameters(context.getContext(), isStatic);

		body.visit(context);
		if(type == DeclarationType.EXPRESSION) mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

		if(type == DeclarationType.STANDARD && !context.getContext().getScope().isReturned()) {
			if(returnType.equals(IxType.VOID_TYPE))
				mv.visitInsn(Opcodes.RETURN);
			else
				throw new IxException(name, "Non-void function must return a value.");
		}

		context.getContext().setScope(outer);

		context.getContext().setType(prev);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private MethodVisitor makeGlobalFunction(Context context) throws IxException {
		int access = verifyAccess();
		String[] exceptions = computeExceptions(context);
		return context.getCurrentClassWriter().visitMethod(access | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, name.value(), descriptor, null, exceptions);
	}

	private MethodVisitor makeClassFunction(Context context) throws IxException {
		int access = verifyAccess();
		int staticAccess = isStatic(context) ? Opcodes.ACC_STATIC : 0;
		String[] exceptions = computeExceptions(context);
		return context.getCurrentClassWriter().visitMethod(access | staticAccess, name.value(), descriptor, null, exceptions);
	}

	private void finalizeMethod(MethodVisitor mv, Context context) throws IxException {
		mv.visitCode();

		if(!returnType.equals(IxType.VOID_TYPE)) mv.visitInsn(returnType.dummyConstant());
		mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}


	private void makeDescriptor(Context context) {
		descriptor = "(%s)%s".formatted(parameters.stream().map(Pair::second).map(n -> Unthrow.wrap(() -> n.getReturnType(context).getDescriptor())).collect(Collectors.joining()), returnType.getDescriptor());
	}

	private void computeReturnType(Context context, boolean isStatic) throws IxException {
		returnType = returnTypeNode == null ? IxType.VOID_TYPE : returnTypeNode.getReturnType(context);

		if(type == DeclarationType.EXPRESSION) {
			ContextType prev = context.getType();

			context.setType(ContextType.FUNCTION);

			Scope outer = context.getScope();

			context.setScope(outer.nextDepth());

			context.getScope().setReturnType(returnType);

			addParameters(context, isStatic);

			returnType = body.getReturnType(context);

			context.setScope(outer);

			context.setType(prev);
		}
	}

	private void addParameters(Context context, boolean isStatic) throws IxException {
		Scope scope = context.getScope();
		if(isStatic) {
			for (Pair<Token, Node> parameter : parameters) {

				IxType parameterType = parameter.second().getReturnType(context);
				scope.addVariable(new Variable(VariableType.LOCAL, parameter.first().value(), scope.nextLocal(), parameterType, false));
				if(parameterType.getSize() == 2)
					scope.nextLocal();
			}
		}
		else {
			scope.nextLocal();
			for (Pair<Token, Node> parameter : parameters) {

				IxType parameterType = parameter.second().getReturnType(context);
				scope.addVariable(new Variable(VariableType.LOCAL, parameter.first().value(), scope.nextLocal(), parameterType, false));

				if(parameterType.getSize() == 2)
					scope.nextLocal();
			}
		}
	}

	private void createMainMethod(Context context) throws IxException {
		if(!"main".equals(name.value())
				|| !parameters.isEmpty()
				|| verifyAccess() != Opcodes.ACC_PUBLIC
				|| !isStatic(context)
				|| !returnType.equals(IxType.VOID_TYPE))
			return;

		String[] exceptions = computeExceptions(context);
		MethodVisitor mainMethodWithArgs = context.getCurrentClassWriter().visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "main", "([Ljava/lang/String;)V", null, exceptions);

		mainMethodWithArgs.visitCode();
		mainMethodWithArgs.visitMethodInsn(Opcodes.INVOKESTATIC, context.getCurrentClass(), "main", "()V", false);
		mainMethodWithArgs.visitInsn(Opcodes.RETURN);
		mainMethodWithArgs.visitMaxs(0, 0);
		mainMethodWithArgs.visitEnd();
	}

	private int verifyAccess() throws IxException {
		if(access == null) return Opcodes.ACC_PUBLIC;
		return switch (access.type()) {
			case PUBLIC -> Opcodes.ACC_PUBLIC;
			case PRIVATE -> Opcodes.ACC_PRIVATE;
			default -> throw new IxException(access, "Invalid access modifier for function '%s'".formatted(access.value()));
		};
	}

	private String[] computeExceptions(Context context) throws IxException {
		if(throwsList == null) return null;

		ArrayList<String> exceptions = new ArrayList<>();
		for(Node exception : throwsList) {
			IxType exceptionType = exception.getReturnType(context);

			if(exceptionType.isPrimitive()) {
				throw new IxException(name, "Cannot throw primitive type (got '%s').".formatted(exceptionType));
			}

			try {
				if(!IxType.getObjectType("java/lang/Throwable").isAssignableFrom(exceptionType, context, false)) {
					throw new IxException(name, "throw target must be an extension of java.lang.Throwable ('%s' cannot be cast).".formatted(exceptionType));
				}
			} catch (ClassNotFoundException e) {
				throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
			exceptions.add(exceptionType.getInternalName());
		}
		return exceptions.toArray(String[]::new);
	}

	private boolean isStatic(Context context) {
		if(staticModifier != null) return true;
        return context.getType() == ContextType.GLOBAL;
    }

	@Override
	public String toString() {
		if(type == DeclarationType.EXPRESSION) {
			return "def %s(%s) => %s;".formatted(name.value(),
					parameters.stream().map(p -> p.first().value() + ": " + p.second()).collect(Collectors.joining(", ")),
					body);
		}

		return "def %s(%s)%s %s".formatted(name.value(),
				parameters.stream().map(p -> p.first().value() + ": " + p.second()).collect(Collectors.joining(", ")),
				returnTypeNode == null ? "" : " -> " + returnTypeNode,
				body);
	}
}
