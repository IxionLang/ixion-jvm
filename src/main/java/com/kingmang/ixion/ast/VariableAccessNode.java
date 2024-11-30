package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.compiler.VariableType;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class VariableAccessNode implements Node {
	private final Token name;
	private boolean isMemberAccess;
	private boolean isStaticClassAccess;

	public VariableAccessNode(Token name) {
		this.name = name;
		this.isMemberAccess = false;
		this.isStaticClassAccess = false;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().updateLine(name.line());
		Variable v = context.getContext().getScope().lookupVariable(name.value());

		if(v == null) {
			if(isMemberAccess) {
				try {
					TypeUtil.classForName(name.value(), context.getContext());
					isStaticClassAccess = true;
					return;
				} catch (ClassNotFoundException e) {
					throw new IxException(name, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
				}
			}
			throw new IxException(name, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
		}

		if(v.getVariableType() == VariableType.STATIC) {
			context.getContext().getMethodVisitor().visitFieldInsn(Opcodes.GETSTATIC, v.getOwner(), v.getName(), v.getType().getDescriptor());
		}
		else if(v.getVariableType() == VariableType.CLASS) {
			if(context.getContext().isStaticMethod())  throw new IxException(name, "Cannot access instance member '%s' in a static context".formatted(name.value()));
			context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
			context.getContext().getMethodVisitor().visitFieldInsn(Opcodes.GETFIELD, v.getOwner(), v.getName(), v.getType().getDescriptor());
		}
		else {
			context.getContext().getMethodVisitor().visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), v.getIndex());
		}
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		Variable v = context.getScope().lookupVariable(name.value());

		if(v == null) {
			if(isMemberAccess) {
				try {
					Class<?> staticClass = TypeUtil.classForName(name.value(), context);
					isStaticClassAccess = true;
					return IxType.getType(staticClass);
				} catch (ClassNotFoundException e) {
					throw new IxException(name, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
				}
			}
			throw new IxException(name, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
		}

		return v.getType();
	}

	@Override
	public Value getLValue() {
		return Value.VARIABLE;
	}

	@Override
	public Object[] getLValueData() {
		return new Object[] { name };
	}

	public void setMemberAccess(boolean memberAccess) {
		isMemberAccess = memberAccess;
	}

	public boolean isStaticClassAccess() {
		return isStaticClassAccess;
	}

	@Override
	public String toString() {
		return name.value();
	}
}
