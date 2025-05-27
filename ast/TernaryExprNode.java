package com.kingmang.ixion.ast;

import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
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

    condition.visit(context);

    trueExpr.visit(context);
    falseExpr.visit(context);

    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "com/kingmang/ixion/runtime/Ternary", "ternaryOperator", "(ZLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
  }

  @Override
  public String toString() {
    return "(%s ? %s : %s)".formatted(condition, trueExpr, falseExpr);
  }

  @Override
  public IxType getReturnType(Context context) throws IxException {
    IxType trueType = trueExpr.getReturnType(context);
    IxType falseType = falseExpr.getReturnType(context);

    if (trueType.equals(falseType)) {
      return trueType;
    }

    throw new IxException(null, "Incompatible types in ternary operator: %s and %s".formatted(trueType, falseType));
  }
}
