package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.*;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.*;

public class VariableDeclarationNode implements Node {

	private final Token name;
	private final Node expectedType;
	private final Node value;
	private final boolean isConst;
	private final Token access;
	private final Token staticModifier;
	private final Token get;
	private final Token set;

	public VariableDeclarationNode(
			Token name,
			Node expectedType,
			Node value,
			boolean isConst,
			Token access,
			Token staticModifier,
			Token get,
			Token set

	) {
		this.name = name;
		this.expectedType = expectedType;
		this.value = value;
		this.isConst = isConst;
		this.access = access;
		this.staticModifier = staticModifier;
		this.get = get;
		this.set = set;
	}

	@Override
	public void preprocess(Context context) throws IxException {
		if(context.getType() == ContextType.GLOBAL) {
			if(context.getScope().lookupVariable(name.value()) != null) throw new IxException(name, "Redefinition of variable '%s' in global scope.".formatted(name.value()));
			defineGetAndSet(true, true, context);

			context.getScope().addVariable(new Variable(VariableType.STATIC, name.value(), "", computeExpectedType(context), isConst));
		}
		else if(context.getType() == ContextType.CLASS) {
			Variable variable = context.getScope().lookupVariable(name.value());
			if(variable != null && variable.getVariableType() == VariableType.CLASS) throw new IxException(name, "Redefinition of variable '%s' within class.".formatted(name.value()));

			boolean isStatic = isStatic(context);

			defineGetAndSet(false, isStatic, context);

			context.getClassVariables().add(this);

			context.getScope().addVariable(new Variable(isStatic ? VariableType.STATIC : VariableType.CLASS, name.value(), context.getCurrentClass(), computeExpectedType(context), isConst));
		}
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType returnType = computeExpectedType(context.getContext());

		if(context.getContext().getType() == ContextType.GLOBAL) {
			defineGetAndSet(true, true, context.getContext());

			context.getContext().setMethodVisitor(context.getContext().getStaticMethodVisitor());
			context.getContext().setStaticMethod(true);
			context.getContext().updateLine(name.line());
			generateValue(context);

			context.getContext().getMethodVisitor().visitFieldInsn(Opcodes.PUTSTATIC, Type.getInternalName(context.getCurrentClass()), name.value(), returnType.getDescriptor());
		}
		else if(context.getContext().getType() == ContextType.FUNCTION) {

			Scope scope = context.getContext().getScope();

			Variable testShadowing = scope.lookupVariable(name.value());

			if(testShadowing != null && testShadowing.getVariableType() == VariableType.LOCAL) {
				throw new IxException(name, "Redefinition of variable '%s' in same scope.".formatted(name.value()));
			}
			context.getContext().updateLine(name.line());
			generateValue(context);


			Variable var = new Variable(VariableType.LOCAL, name.value(), scope.nextLocal(), returnType, isConst);
			scope.addVariable(var);

			context.getContext().getMethodVisitor().visitVarInsn(returnType.getOpcode(Opcodes.ISTORE), var.getIndex());

			if(returnType.getSize() == 2) scope.nextLocal();
		}
		else if(context.getContext().getType() == ContextType.CLASS) {
			if(!context.getContext().isConstructor()) defineGetAndSet(false, isStatic(context.getContext()), context.getContext());

			if(isStatic(context.getContext())) {
				context.getContext().setMethodVisitor(context.getContext().getStaticMethodVisitor());
				context.getContext().setStaticMethod(true);
				return;
			}

			if(context.getContext().getMethodVisitor() == null) return;

			context.getContext().setMethodVisitor(context.getContext().getDefaultConstructor());
			context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
			context.getContext().setStaticMethod(false);
			context.getContext().updateLine(name.line());

			generateValue(context);

			int setOpcode = isStatic(context.getContext()) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;

			context.getContext().getMethodVisitor().visitFieldInsn(setOpcode, Type.getInternalName(context.getCurrentClass()), name.value(), returnType.getDescriptor());
		}
	}

	private void defineGetAndSet(boolean isFinal, boolean isStatic, Context context) throws IxException {

		int finalMod = isConst ? Opcodes.ACC_FINAL : 0;
		int staticMod = isStatic ? Opcodes.ACC_STATIC : 0;
		int methodFinalMod = isFinal ? Opcodes.ACC_FINAL : 0;

		IxType fieldType = computeExpectedType(context);
		String descriptor = fieldType.getDescriptor();

		FieldVisitor fv = context.getCurrentClassWriter().visitField(Opcodes.ACC_PRIVATE | staticMod | finalMod, name.value(), descriptor, null, null);

		fv.visitEnd();

		String beanName = name.value().substring(0, 1).toUpperCase() + name.value().substring(1);

		if(get != null)
			visitGetter(context, fieldType, isStatic, descriptor, beanName, staticMod, methodFinalMod);

		if(set != null)
			visitSetter(context, fieldType, isStatic, descriptor, beanName, staticMod, methodFinalMod);

	}

	private void visitSetter(Context context, IxType fieldType, boolean isStatic, String descriptor, String beanName, int staticMod, int methodFinalMod) throws IxException {
		int setOpcode = isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
		if(verifyAccess() && !isConst) {
			String fName = "set" + (name.value().matches("^is\\p{Lu}.*") ? beanName.substring(2) : beanName);
			MethodVisitor visitor = context.getCurrentClassWriter().visitMethod(Opcodes.ACC_PUBLIC | staticMod | methodFinalMod, fName, "("  + descriptor + ")V", null, null);
			visitor.visitCode();

			if(!isStatic) visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitVarInsn(fieldType.getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1);
			visitor.visitFieldInsn(setOpcode, context.getCurrentClass(), name.value(), descriptor);
			visitor.visitInsn(Opcodes.RETURN);
			visitor.visitMaxs(1, 1);
			visitor.visitEnd();
		}
	}

	private void visitGetter(Context context, IxType fieldType, boolean isStatic, String descriptor, String beanName, int staticMod, int methodFinalMod) throws IxException {
		int getOpcode = isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
		if(verifyAccess()) {
			String fName = name.value().matches("^is\\p{Lu}.*") ? name.value() : "get" + beanName;
			MethodVisitor visitor = context.getCurrentClassWriter().visitMethod(Opcodes.ACC_PUBLIC | staticMod | methodFinalMod, fName, "()" + descriptor, null, null);
			visitor.visitCode();

			if(!isStatic) visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitFieldInsn(getOpcode, context.getCurrentClass(), name.value(), descriptor);
			visitor.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
			visitor.visitMaxs(1, 0);
			visitor.visitEnd();
		}
	}


	private boolean verifyAccess() throws IxException {
		if(access == null) return true;
		return switch (access.type()) {
			case PUBLIC -> true;
			case PRIVATE -> false;
			default -> throw new IxException(access, "Invalid access modifier for variable '%s'".formatted(access.value()));
		};
	}

	private IxType computeExpectedType(Context context) throws IxException {
		if(expectedType == null) {
			return value.getReturnType(context);
		}
		else if(value == null) {
			return expectedType.getReturnType(context);
		}

		IxType expected = expectedType.getReturnType(context);
		IxType valueType = value.getReturnType(context);

		try {
			if(!expected.isAssignableFrom(valueType, context, false)) {
				throw new IxException(name, "Cannot assign type of '%s' to annotated type of '%s'.".formatted(
						valueType,
						expected
				));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		return expected;
	}

	private void generateValue(FileContext context) throws IxException {
		if(value == null) {
			IxType returnType = expectedType.getReturnType(context.getContext());
			if(!returnType.isNullable() && !returnType.isPrimitive()) throw new IxException(name, "Cannot default initialize variable of type '%s'".formatted(
					returnType
			));
			context.getContext().getMethodVisitor().visitInsn(returnType.dummyConstant());
		}
		else {
			value.visit(context);
		}
	}

	public boolean isStatic(Context context) {
		return staticModifier != null || context.getType() == ContextType.GLOBAL;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder((isConst ? "const " : "var ") + name.value());
		if(expectedType != null) {
			builder.append(": ").append(expectedType);
		}
		if(value != null) {
			builder.append(" = ").append(value);
		}
		builder.append(";");
		return builder.toString();
	}
}
