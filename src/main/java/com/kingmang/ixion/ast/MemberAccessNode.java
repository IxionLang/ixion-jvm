package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MemberAccessNode implements Node {

	private final Node left;
	private final Token name;
	private boolean isStaticAccess;

	public MemberAccessNode(Node left, Token name) {
		this.left = left;
		this.name = name;
		this.isStaticAccess = false;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		getLeftType(context.getContext());
		left.visit(context);

		IxType returnType = left.getReturnType(context.getContext());
		if(returnType.isNullable()) {
			throw new IxException(name, "Cannot use '.' to access members on a nullable type ('%s')".formatted(returnType));
		}

		visitAccess(context);
	}

	public void visitAccess(FileContext context) throws IxException {
		IxType leftType = getLeftType(context.getContext());

		if(leftType.isArray() && name.value().equals("length")) {
			context.getContext().getMethodVisitor().visitInsn(Opcodes.ARRAYLENGTH);
			return;
		}

		if(!leftType.isObject()) {
			throw new IxException(name, "Cannot access member on type '%s'".formatted(leftType));
		}

		resolve(leftType, context.getContext(), true);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType leftType = getLeftType(context);

		if(leftType.isArray() && name.value().equals("length")) return IxType.INT_TYPE;

		return resolve(leftType, context, false);
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

	private IxType resolve(IxType leftType, Context context, boolean generate) throws IxException {
		Class<?> klass;

		try {
			klass = Class.forName(leftType.getClassName(), false, context.getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		try {
			Field f = klass.getDeclaredField(name.value());

			if(!Modifier.isPublic(f.getModifiers()) && !leftType.equals(IxType.getObjectType(context.getCurrentClass()))) {
				throw new NoSuchFieldException();
			}

			if(!isStaticAccess && Modifier.isStatic(f.getModifiers())) {
				throw new IxException(name, "Cannot access static member from non-static object.");
			}

			if(generate) {
				context.getMethodVisitor().visitFieldInsn(TypeUtil.getAccessOpcode(f),
						leftType.getInternalName(), name.value(), Type.getType(f.getType()).getDescriptor());
			}

			return IxType.getType(f.getType());
		} catch (NoSuchFieldException e) {
			String base = name.value();
			String getName = "get" + base.substring(0, 1).toUpperCase() + base.substring(1);

			try {
				return attemptMethodCall(klass, leftType, getName, generate, context);
			} catch (NoSuchMethodException noSuchMethodException) {
				try {
					return attemptMethodCall(klass, leftType, base, generate, context);
				} catch (NoSuchMethodException suchMethodException) {
					throw new IxException(name, "Could not resolve field '%s' in class '%s'".formatted(name.value(), leftType));
				}
			}
		}
	}

	private IxType attemptMethodCall(Class<?> klass, IxType leftType, String methodName, boolean generate, Context context) throws NoSuchMethodException, IxException {
		Method m = klass.getMethod(methodName);

		IxType returnType = IxType.getType(m).getReturnType();

		if(!isStaticAccess && Modifier.isStatic(m.getModifiers())) {
			throw new IxException(name, "Cannot access static member from non-static object.");
		}

		if(generate) {
			context.getMethodVisitor().visitMethodInsn(TypeUtil.getInvokeOpcode(m),
					leftType.getInternalName(), methodName, "()" + returnType.getDescriptor(), false);
		}

		return returnType;
	}

	@Override
	public Value getLValue() {
		return Value.PROPERTY;
	}

	@Override
	public Object[] getLValueData() {
		return new Object[] { left, name };
	}

	@Override
	public String toString() {
		return "%s.%s".formatted(left, name.value());
	}
}
