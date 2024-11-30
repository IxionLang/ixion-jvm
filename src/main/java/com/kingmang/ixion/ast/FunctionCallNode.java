package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionCallNode implements Node {

	private final Token name;
	private final List<Node> args;

	public FunctionCallNode(Token name, List<Node> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().updateLine(name.line());
		IxType[] argTypes = new IxType[args.size()];

		for(int i = 0; i < args.size(); i++) {
			Node arg = args.get(i);

			argTypes[i] = arg.getReturnType(context.getContext());
		}

		try {
			IxFunction function = context.getContext().getScope().lookupFunction(name.value(), argTypes);

			if(function == null) throw new IxException(name,
					"Could not resolve function '%s' with arguments: %s".formatted(name.value(),
							argTypes.length == 0 ? "(none)" :
							Stream.of(argTypes).map(IxType::toString).collect(Collectors.joining(", "))));

			if(function.functionType() == IxFunctionType.OUTPUT)
				context.getContext().getMethodVisitor().visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			else if(function.functionType() == IxFunctionType.CLASS) {
				if(context.getContext().isStaticMethod()) throw new IxException(name, "Cannot invoke instance method '%s' from static context.".formatted(name.value()));
				context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
			}

			context.getContext().getScope().lookupFunction(name.value(), argTypes, args.toArray(Node[]::new), true, context);

			context.getContext().getMethodVisitor().visitMethodInsn(function.getAccess(), function.parent(), function.name(), function.type().getDescriptor(), false);

		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType[] argTypes = new IxType[args.size()];

		for(int i = 0; i < args.size(); i++) {
			Node arg = args.get(i);

			argTypes[i] = arg.getReturnType(context);
		}

		try {
			IxFunction function = context.getScope().lookupFunction(name.value(), argTypes);

			if(function == null) throw new IxException(name,
					"Could not resolve function '%s' with arguments: %s".formatted(name.value(),
							argTypes.length == 0 ? "(none)" :
									Stream.of(argTypes).map(IxType::toString).collect(Collectors.joining(", "))));

			return function.type().getReturnType();

		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
	}

	@Override
	public String toString() {
		return name.value() + "(" + args.stream().map(Node::toString).collect(Collectors.joining(", ")) + ")";
	}
}
