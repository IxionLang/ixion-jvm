package com.kingmang.ixion.ast;

import com.kingmang.ixion.compiler.*;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LambdaDeclarationNode implements Node {
    private final Token token;
    private final List<Pair<Token, Node>> parameters;
    private final Node body;
    private List<IxType> parameterTypes;
    private IxType returnType;
    @Setter
    private IxType itf;

    public LambdaDeclarationNode(
            Token token,
            List<Pair<Token, Node>> parameters,
            Node body) {
        this.token = token;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public void visit(FileContext context) throws IxException {
        if (itf == null)
            itf = IxType.getType("Lcom/kingmang/ixion/runtime/functions/Function" + this.parameters.size() + ";");
        //
        boolean inStatic = context.getContext().isStaticMethod();
        String lambdaName = "_lambda$" + Math.abs(this.body.hashCode() * 31 + this.parameters.hashCode());
        String lambdaDesc;
        Method itfMethod;
        try {
            itfMethod = Arrays.stream(itf.toClass(context.getContext()).getMethods()).filter(it -> Modifier.isAbstract(it.getModifiers())).findFirst().get();
        } catch (ClassNotFoundException e) {
            throw new IxException(token, "Could not resolve class '%s'".formatted(e.getMessage()));
        }
        // Параметры
        parameterTypes = new ArrayList<>();
        for (Pair<Token, Node> pair : parameters) {
            Node parameterTypeNode = pair.second();
            parameterTypes.add(parameterTypeNode == null ? IxType.OBJECT_TYPE : parameterTypeNode.getReturnType(context.getContext()));
        }
        // Определение
        {
            Scope outer = context.getContext().getScope();
            ContextType prevType = context.getContext().getType();
            MethodVisitor prevVisitor = context.getContext().getMethodVisitor();
            Scope scope = outer.nextDepth();
            scope.setReturnType(IxType.OBJECT_TYPE);
            context.getContext().setScope(scope);
            context.getContext().setType(ContextType.FUNCTION);
            context.getContext().setStaticMethod(inStatic);
            //
            if (!inStatic)
                scope.addVariable(new Variable(VariableType.LOCAL, "this", scope.nextLocal(), IxType.OBJECT_TYPE, true));
            for (int i = 0; i < parameters.size(); i++) {
                IxType parameterType = parameterTypes.get(i);
                scope.addVariable(new Variable(VariableType.LOCAL, parameters.get(i).first().value(), scope.nextLocal(), parameterType, false));
                if (parameterType.getSize() == 2) {
                    scope.nextLocal();
                }
            }
            returnType = body.getReturnType(context.getContext());
            lambdaDesc = "(%s)%s".formatted(parameterTypes.stream().map(IxType::getDescriptor).collect(Collectors.joining()), (returnType == IxType.VOID_TYPE && !(itfMethod.getReturnType() == void.class)) ? "Ljava/lang/Object;" : returnType.getDescriptor());
            //
            MethodVisitor mv = context.getContext().getCurrentClassWriter().visitMethod(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                    lambdaName,
                    lambdaDesc,
                    null,
                    null
            );
            mv.visitCode();
            context.getContext().setMethodVisitor(mv);
            body.visit(context);
            if (returnType == IxType.VOID_TYPE) {
                if (itfMethod.getReturnType() == void.class) {
                    mv.visitInsn(Opcodes.RETURN);
                } else {
                    returnType = IxType.OBJECT_TYPE;
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
                }
            } else {
                mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
            }
            mv.visitEnd();
            mv.visitMaxs(0, 0);
            //
            context.getContext().setScope(outer);
            context.getContext().setType(prevType);
            context.getContext().setMethodVisitor(prevVisitor);
        }
        // Аллокация
        {
            String declaration = context.getContext().getCurrentClass().replace('.', '/');
            MethodVisitor mv = context.getContext().getMethodVisitor();
            if (!inStatic)
                mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInvokeDynamicInsn(
                    itfMethod.getName(),
                    "(" + (inStatic ? "" : ("L" + declaration + ";")) + ")" + itf.getDescriptor(),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            "java/lang/invoke/LambdaMetafactory",
                            "metafactory",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                            false
                    ),
                    Type.getType("(%s)%s".formatted(Arrays.stream(IxType.getType(itfMethod).getArgumentTypes()).map(IxType::getDescriptor).collect(Collectors.joining()), Type.getType(itfMethod.getReturnType()).getDescriptor())),
                    new Handle(Opcodes.H_INVOKESTATIC, declaration, lambdaName, lambdaDesc, false),
                    Type.getType("(%s)%s".formatted(parameterTypes.stream().map(it -> it.getAutoBoxWrapper().getDescriptor()).collect(Collectors.joining()), returnType == IxType.VOID_TYPE ? "V" : returnType.getAutoBoxWrapper().getDescriptor()))
            );
        }
    }

    @Override
    public IxType getReturnType(Context context) throws IxException {
        return itf == null ? IxType.getType("Lcom/kingmang/ixion/runtime/functions/Function" + this.parameters.size() + ";") : itf;
    }
}
