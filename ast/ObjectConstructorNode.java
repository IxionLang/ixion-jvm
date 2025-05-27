package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.util.Unthrow;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectConstructorNode implements Node {

	private final Token newToken;
	private final Node type;
	private final List<Node> arguments;

	public ObjectConstructorNode(Token newToken, Node type, List<Node> arguments) {
		this.newToken = newToken;
		this.type = type;
		this.arguments = arguments;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType objType = type.getReturnType(context.getContext());

		if(objType.isPrimitive()) {
			throw new IxException(newToken, "Cannot create new instance of primitive type (%s)"
				.formatted(objType));
		}

		context.getContext().updateLine(newToken.line());

		IxType[] argTypes = arguments.stream()
				.map(n -> Unthrow.wrap(() -> n.getReturnType(context.getContext()))).toArray(IxType[]::new);

		Class<?> klass;

		try {
			klass = Class.forName(objType.getClassName(), false, context.getContext().getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(newToken, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		Constructor<?> toCall = TypeUtil.getConstructor(newToken, klass.getConstructors(), argTypes, context.getContext());

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();
		methodVisitor.visitTypeInsn(Opcodes.NEW, objType.getInternalName());
		methodVisitor.visitInsn(Opcodes.DUP);

		String descriptor;

		if(toCall == null) {
			Constructor<?> declaredConstructor = TypeUtil.getConstructor(newToken, klass.getDeclaredConstructors(), argTypes, context.getContext());
			if(declaredConstructor == null) throw new IxException(newToken, "Class '%s' cannot be instantiated with arguments: %s"
					.formatted(objType,
							argTypes.length == 0 ? "(none)" : Arrays.stream(argTypes).map(IxType::toString).collect(Collectors.joining(", "))));

			int modifiers = declaredConstructor.getModifiers();
			if(Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)) {
				throw new IxException(newToken, "Class '%s' does not have a public constructor".formatted(objType));
			}

			descriptor = "(%s)V".formatted(
					Arrays.stream(declaredConstructor.getParameterTypes()).map(Type::getType).map(Type::getDescriptor).collect(Collectors.joining()));
			toCall = declaredConstructor;
		}
		else {
			descriptor = "(%s)V".formatted(
					Arrays.stream(toCall.getParameterTypes()).map(Type::getType).map(Type::getDescriptor).collect(Collectors.joining()));
		}
		IxType[] resolvedTypes = IxType.getType(toCall).getArgumentTypes();
		for(int i = 0; i < arguments.size(); i++) {
			Node arg = arguments.get(i);
			IxType resolvedType = resolvedTypes[i];

			arg.visit(context);
			try {
				resolvedType.isAssignableFrom(arg.getReturnType(context.getContext()), context.getContext(), true);
			} catch (ClassNotFoundException e) {
				throw new IxException(newToken, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
		}

		methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
				objType.getInternalName(), "<init>", descriptor, false);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return type.getReturnType(context);
	}

	@Override
	public String toString() {
		return "new %s(%s)".formatted(type, arguments.stream().map(Node::toString).collect(Collectors.joining(", ")));
	}
}
