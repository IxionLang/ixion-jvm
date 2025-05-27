package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.*;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.util.Unthrow;

import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;
import lombok.Setter;
import org.objectweb.asm.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ThisMethodDeclarationNode implements Node {
	private final Token access;
	private final Token constructorToken;
	private final List<Node> superArgs;
	private final List<Pair<Token, Node>> parameters;
	private final Node body;
	@Setter
    private List<VariableDeclarationNode> variablesInit;

	public ThisMethodDeclarationNode(Token access, Token constructor, List<Node> superArgs, List<Pair<Token, Node>> parameters, Node body) {
		this.access = access;
		this.constructorToken = constructor;
		this.superArgs = superArgs;
		this.parameters = parameters;
		this.body = body;
		this.variablesInit = new ArrayList<>();
	}

	@Override
	public void visit(FileContext fc) throws IxException {
		Context context = fc.getContext();

		ClassWriter writer = context.getCurrentClassWriter();
		String args = parameters.stream().map(Pair::second).map(n -> Unthrow.wrap(() -> n.getReturnType(context).getDescriptor())).collect(Collectors.joining());
		MethodVisitor constructor = writer.visitMethod(getAccess(), "<init>", "(" + args + ")V", null, null);
		constructor.visitCode();

		context.setDefaultConstructor(constructor);
		context.setMethodVisitor(constructor);
		context.setConstructor(true);

		constructor.visitVarInsn(Opcodes.ALOAD, 0);

		Scope outer = context.getScope();

		Scope inner = outer.nextDepth();

		context.setScope(inner);

		context.getScope().nextLocal();

		for (Pair<Token, Node> parameter : parameters) {
			Scope scope = context.getScope();
			IxType parameterType = parameter.second().getReturnType(context);
			scope.addVariable(new Variable(VariableType.LOCAL, parameter.first().value(), scope.nextLocal(), parameterType, false));
			if(parameterType.getSize() == 2)
				scope.nextLocal();
		}

		createSuperCall(constructor, fc);

		context.setScope(outer);

		for(Node variable : variablesInit) {
			variable.visit(fc);
			context.setMethodVisitor(constructor);
		}

		ContextType prev = context.getType();
		context.setType(ContextType.FUNCTION);
		context.setScope(inner);

		body.visit(fc);

		context.setScope(outer);

		context.setType(prev);

		context.setConstructor(false);

		constructor.visitInsn(Opcodes.RETURN);
		constructor.visitMaxs(0, 0);
		constructor.visitEnd();
	}

	@Override
	public void preprocess(Context context) throws IxException {
		MethodVisitor constructor = createDefaultConstructor(context);
		constructor.visitInsn(Opcodes.RETURN);
		constructor.visitMaxs(0, 0);
		constructor.visitEnd();
	}

	private MethodVisitor createDefaultConstructor(Context context) throws IxException {
		ClassWriter writer = context.getCurrentClassWriter();
		String args = parameters.stream().map(Pair::second).map(n -> Unthrow.wrap(() -> n.getReturnType(context).getDescriptor())).collect(Collectors.joining());
		MethodVisitor constructor = writer.visitMethod(getAccess(), "<init>", "(" + args + ")V", null, null);
		constructor.visitCode();


		constructor.visitVarInsn(Opcodes.ALOAD, 0);
		constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, context.getCurrentSuperClass().getInternalName(), "<init>", "()V", false);

		return constructor;
	}

	private void createSuperCall(MethodVisitor methodVisitor, FileContext fc) throws IxException {
		Context context = fc.getContext();
		IxType[] argTypes;
		if(superArgs == null) {
			argTypes = new IxType[] {};
		}
		else {
			argTypes = superArgs.stream().map(n -> Unthrow.wrap(() -> n.getReturnType(context))).toArray(IxType[]::new);
		}

		Class<?> klass;

		try {
			klass = Class.forName(context.getCurrentSuperClass().getClassName(), false, context.getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(constructorToken, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		Constructor<?>[] constructors = Arrays.stream(klass.getDeclaredConstructors()).filter(c -> !Modifier.isPrivate(c.getModifiers())).toArray(Constructor[]::new);

		Constructor<?> superConstructor = TypeUtil.getConstructor(constructorToken, constructors, argTypes, context);

		if(superConstructor == null) throw new IxException(constructorToken, "SuperClass '%s' cannot be instantiated with arguments: %s"
				.formatted(context.getCurrentSuperClass(),
						Arrays.stream(argTypes).map(IxType::toString).collect(Collectors.joining(", "))));

		IxType[] resolvedTypes = IxType.getType(superConstructor).getArgumentTypes();
		if(superArgs != null) {
			for (int i = 0; i < superArgs.size(); i++) {
				Node arg = superArgs.get(i);
				IxType resolvedType = resolvedTypes[i];

				arg.visit(fc);
				try {
					resolvedType.isAssignableFrom(arg.getReturnType(context), context, true);
				} catch (ClassNotFoundException e) {
					throw new IxException(constructorToken, "Could not resolve class '%s'".formatted(e.getMessage()));
				}
			}
		}

		String descriptor = "(%s)V".formatted(Arrays.stream(
				superConstructor.getParameterTypes()).map(Type::getType).map(Type::getDescriptor).collect(Collectors.joining()));

		methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, context.getCurrentSuperClass().getInternalName(), "<init>", descriptor, false);
	}

	private int getAccess() {
		if(access == null || access.type() == TokenType.PUBLIC) return Opcodes.ACC_PUBLIC;
		return Opcodes.ACC_PRIVATE;
	}

    @Override
	public String toString() {
		return "this(%s) %s".formatted(
				parameters.stream().map(p -> p.first().value() + ": " + p.second()).collect(Collectors.joining(", ")),
				body
		);
	}
}
