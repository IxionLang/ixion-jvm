package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TernaryExprNode implements Node {
  
  private final Node condition;
  private final Node trueExpr;
  private final Node falseExpr;

  public TernaryExprNode(Node condition, Node trueExpr, Node falseExpr) {
    this.condition = condition;
    this.trueExpr = trueExpr;
    this.falseExpr = falseExpr;
  }

  @Override
  public void visit(FileContext context) throws IxException {
    MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

    Label falseLabel = new Label();
    Label endLabel = new Label();

    condition.visit(context);
    methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseLabel);

    trueExpr.visit(context);
    methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);


    methodVisitor.visitLabel(falseLabel);
    falseExpr.visit(context);

    methodVisitor.visitLabel(endLabel);
  }

  @Override
  public String toString() {
    return "(%s ? %s : %s)".formatted(condition, trueExpr, falseExpr);
  }
}
