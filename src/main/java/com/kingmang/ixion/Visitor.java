package com.kingmang.ixion;

public interface Visitor<R> extends StatementVisitor<R>, ExprVisitor<R> {}
