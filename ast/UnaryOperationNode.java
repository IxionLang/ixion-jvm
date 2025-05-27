package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class UnaryOperationNode implements Node {
	private final Token op;
	private final Node expression;

	public UnaryOperationNode(Token op, Node expression) {
		this.op = op;
		this.expression = expression;
	}

	@Override
	public void visit(FileContext context) throws IxException {

		if(context.shouldOptimize("constant.unary") && isConstant(context.getContext())) {
			TypeUtil.correctLdc(getConstantValue(context.getContext()), context.getContext());
			return;
		}

		expression.visit(context);

		IxType expressionType = expression.getReturnType(context.getContext());

		MethodVisitor mv = context.getContext().getMethodVisitor();

		if(op.type() == TokenType.EXCLAIM) {
			if(!expressionType.equals(IxType.BOOLEAN_TYPE))
				throw new IxException(op, "Can only perform '!' on boolean values. (%s =/= boolean)".formatted(expressionType));

			Label falseL = new Label();
			Label end = new Label();

			mv.visitJumpInsn(Opcodes.IFNE, falseL);
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitJumpInsn(Opcodes.GOTO, end);
			mv.visitLabel(falseL);
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLabel(end);
		}
		else if(op.type() == TokenType.MINUS) {
			if(!expressionType.isNumeric()) {
				throw new IxException(op, "Can only perform '-' on numeric values. (%s is not numeric)".formatted(expressionType));
			}


			mv.visitInsn(expressionType.getOpcode(Opcodes.INEG));
		}
		else if(op.type() == TokenType.BITWISE_NOT) {
			if(!expressionType.isInteger()) {
				throw new IxException(op,
						"Can only perform '~' on integer values. ('%s' is not an integer)".formatted(expressionType));
			}

			if(expressionType.equals(IxType.LONG_TYPE)) {
				TypeUtil.generateCorrectLong(-1, context.getContext());
				mv.visitInsn(Opcodes.LXOR);
			}
			else {
				TypeUtil.generateCorrectInt(-1, context.getContext());
				mv.visitInsn(Opcodes.IXOR);
			}
		}
	}

	@Override
	public Object getConstantValue(Context context) throws IxException {
		Object value = expression.getConstantValue(context);

		if(op.type() == TokenType.EXCLAIM) {
			IxType returnType = expression.getReturnType(context);
			if(!returnType.equals(IxType.BOOLEAN_TYPE))
				throw new IxException(op, "Con only perform '!' on boolean values. (%s =/= boolean)".formatted(returnType));

			boolean boolVal = (Boolean) value;

			return !boolVal;
		}
		else if(op.type() == TokenType.MINUS) {
			IxType returnType = expression.getReturnType(context);
			if(!returnType.isNumeric()) {
				throw new IxException(op, "Con only perform '-' on numeric values. (%s is not numeric)".formatted(returnType));
			}

			if(value instanceof Double) {
				return -(double)value;
			}
			else if(value instanceof Float) {
				return -(float)value;
			}
			else if(value instanceof Integer) {
				return -(int)value;
			}
			else if(value instanceof Long) {
				return -(long)value;
			}
		}

		throw new IllegalStateException("Unknown unary operator " + op.type());
	}

	@Override
	public boolean isConstant(Context context) throws IxException {
		if(op.type() == TokenType.MINUS) {
			IxType type = expression.getReturnType(context);
			return switch(type.getSort()) {
				case DOUBLE, FLOAT, INT, LONG -> true;
				default -> false;
			};
		}
		else if(op.type() == TokenType.BITWISE_NOT) return false;
		return expression.isConstant(context);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return switch (op.type()) {
			case EXCLAIM -> IxType.BOOLEAN_TYPE;
			case MINUS, BITWISE_NOT -> expression.getReturnType(context);
			default -> throw new IllegalStateException("Unknown unary operator " + op.type());
		};
	}

	@Override
	public String toString() {
		return op.value() + expression.toString();
	}
}
