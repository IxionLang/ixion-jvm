package com.kingmang.ixion.ast;

import com.kingmang.ixion.class_utils.CustomClassWriter;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.ContextType;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.util.FileContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class TraitDeclarationNode implements Node {
    private final Token name;
    private final List<Node> methods;
    private final Token access;

    public TraitDeclarationNode(Token name, List<Node> methods, Token access) {
        this.name = name;
        this.methods = methods;
        this.access = access;
    }

    @Override
    public void visit(FileContext fc) throws IxException {
        Context context = fc.getContext();

        ContextType prevType = context.getType();
        String prevClass = context.getCurrentClass();

        ClassWriter writer = initTraitClass(context);
        context.setType(ContextType.CLASS);

        for(Node node : methods)
            if(node instanceof FunctionDeclarationNode funcNode)
                funcNode.preprocess(context);
            else
                throw new IxException(name, "Exception in using node");

        MethodVisitor staticMethod = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        staticMethod.visitCode();

        context.setMethodVisitor(staticMethod);


        staticMethod.visitInsn(Opcodes.RETURN);
        staticMethod.visitMaxs(0, 0);
        staticMethod.visitEnd();

        writer.visitEnd();
        context.setCurrentClass(prevClass);
        context.setType(prevType);
    }
    @Override
    public boolean isNewClass() {
        return true;
    }

    @Override
    public void buildClasses(Context context) {
        String prevClass = context.getCurrentClass();

        ClassWriter writer = initTraitClass(context);
        writer.visitEnd();

        context.setCurrentClass(prevClass);
    }

    private ClassWriter initTraitClass(Context context) {
        int accessLevel = getAccessLevel();

        String baseName = name.value();
        String className = baseName;

        if (accessLevel == 0) {
            className = context.getCurrentClass() + "$" + baseName;
        }
        if (!context.getPackageName().isEmpty()) {
            className = context.getPackageName() + "/" + className;
        }
        if (!className.equals(baseName)) {
            context.getUsings().put(baseName, className.replace('/', '.'));
        }

        context.setCurrentClass(className);

        ClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, context.getLoader());

        writer.visit(Opcodes.V9, accessLevel | Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, className, null, "java/lang/Object", null);

        writer.visitSource(context.getSource(), null);

        context.getClassWriterMap().put(className, writer);

        return writer;
    }
    private int getAccessLevel() {
        if(access == null || access.type() == TokenType.PUBLIC) return Opcodes.ACC_PUBLIC;
        return 0;
    }



}
