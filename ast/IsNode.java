package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;

public class IsNode implements Node {

	private final Node expression;
	private final Token op;
	private final Node type;
	@Setter
    private boolean shouldReScope;
	@Getter
    private Pair<Variable, IxType> pastVariable;

	public IsNode(Node expression, Token op, Node type) {
		this.expression = expression;
		this.op = op;
		this.type = type;
		this.shouldReScope = false;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType objectType = expression.getReturnType(context.getContext());

		if(!objectType.isObject()) {
			throw new IxException(op, "Can only perform 'instanceof' on objects (got '%s')".formatted(objectType));
		}

		IxType expectedType = type.getReturnType(context.getContext());

		if(!expectedType.isObject()) {
			throw new IxException(op, "Cannot check for an instance of type '%s'".formatted(expectedType));
		}

		if(expectedType.isNullable()) {
			throw new IxException(op, "Cannot check for an instance of a nullable type ('%s')".formatted(expectedType));
		}

		expression.visit(context);

		context.getContext().getMethodVisitor().visitTypeInsn(Opcodes.INSTANCEOF, expectedType.getInternalName());

		if(shouldReScope && expression instanceof VariableAccessNode) {
			Variable v = context.getContext().getScope().lookupVariable(((Token) expression.getLValueData()[0]).value());

			pastVariable = new Pair<>(v, v.getType());

			try {
				if(!v.getType().isAssignableFrom(expectedType, context.getContext(), false)
				&& !expectedType.isAssignableFrom(v.getType(), context.getContext(), false)) {
					throw new IxException(op, "Cannot check for an instance between '%s' and '%s'".formatted(v.getType(), expectedType));
				}
			} catch (ClassNotFoundException e) {
				throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
			v.setType(expectedType);
		}
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.BOOLEAN_TYPE;
	}

	@Override
	public String toString() {
		return "%s is %s".formatted(expression, type);
	}

}
