package com.kingmang.ixion.ast;

import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.util.FileContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CycleControlNode implements Node {
    private final Token op;

    public CycleControlNode(Token op) {
        this.op = op;
    }

    @Override
    public void visit(FileContext context) throws IxException {
        Label start = context.getContext().getCycleStartLabel();
        if (start == null)
            throw new IxException(op, "Uses of 'break' or 'continue' without cycle");
        MethodVisitor mv = context.getContext().getMethodVisitor();
        switch (op.type()) {
            case BREAK -> mv.visitJumpInsn(Opcodes.GOTO, context.getContext().getCycleEndLabel());
            case CONTINUE -> mv.visitJumpInsn(Opcodes.GOTO, start);
            default -> throw new IxException(op, "Unsupported operation type");
        }
    }
}
