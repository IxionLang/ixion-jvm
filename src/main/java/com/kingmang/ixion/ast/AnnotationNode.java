package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;

public class AnnotationNode implements Node {
    private final Token name;
    private final List<Node> arguments;
    private final Map<String, Node> namedArguments;

    public AnnotationNode(Token name, List<Node> arguments, Map<String, Node> namedArguments) {
        this.name = name;
        this.arguments = arguments;
        this.namedArguments = namedArguments;
    }

    @Override
    public void visit(FileContext context) throws IxException {}

    @Override
    public void preprocess(Context context) throws IxException {
        context.addAnnotation(this);
    }

    public void applyToClass(ClassVisitor visitor, Context context) throws IxException {
        String annotationName = name.value();
        String fullName = context.getUsings().getOrDefault(annotationName, annotationName);
        AnnotationVisitor annotationVisitor = visitor.visitAnnotation("L" + fullName.replace('.', '/') + ";", true);
        visitAnnotationValues(annotationVisitor, context);
        annotationVisitor.visitEnd();
    }

    public void applyToMethod(MethodVisitor visitor, Context context) throws IxException {
        String annotationName = name.value();
        String fullName = context.getUsings().getOrDefault(annotationName, annotationName);
        AnnotationVisitor annotationVisitor = visitor.visitAnnotation("L" + fullName.replace('.', '/') + ";", true);
        visitAnnotationValues(annotationVisitor, context);
        annotationVisitor.visitEnd();
    }

    public void applyToField(FieldVisitor visitor, Context context) throws IxException {
        String annotationName = name.value();
        String fullName = context.getUsings().getOrDefault(annotationName, annotationName);
        AnnotationVisitor annotationVisitor = visitor.visitAnnotation("L" + fullName.replace('.', '/') + ";", true);
        visitAnnotationValues(annotationVisitor, context);
        annotationVisitor.visitEnd();
    }

    private void visitAnnotationValues(AnnotationVisitor visitor, Context context) throws IxException {
        for (Node arg : arguments) {
            if (arg.isConstant(context)) {
                Object value = arg.getConstantValue(context);
                if (value != null) {
                    visitor.visit("", value);
                }
            } else {
                throw new IxException(name, "Annotation arguments must be constant expressions");
            }
        }

        for (Map.Entry<String, Node> entry : namedArguments.entrySet()) {
            String key = entry.getKey();
            Node value = entry.getValue();
            if (value.isConstant(context)) {
                Object constValue = value.getConstantValue(context);
                if (constValue != null) {
                    visitor.visit(key, constValue);
                }
            } else {
                throw new IxException(name, "Annotation arguments must be constant expressions");
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("@").append(name.value());
        if (!arguments.isEmpty() || !namedArguments.isEmpty()) {
            sb.append("(");
            boolean first = true;

            for (Node arg : arguments) {
                if (!first) sb.append(", ");
                sb.append(arg);
                first = false;
            }

            for (Map.Entry<String, Node> entry : namedArguments.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append(" = ").append(entry.getValue());
                first = false;
            }
            
            sb.append(")");
        }
        return sb.toString();
    }
} 