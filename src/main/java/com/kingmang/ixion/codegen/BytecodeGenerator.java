package com.kingmang.ixion.codegen;

import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.runtime.StructType;
import org.apache.commons.io.FilenameUtils;
import org.javatuples.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Map;

public class BytecodeGenerator {

    public static void addToString(ClassWriter cw, StructType st, String constructorDescriptor, String ownerInternalName) {}

    public Pair<ClassWriter, Map<StructType, ClassWriter>> generate(IxApi ixApi, IxFile source) {
        var cw = new ClassWriter(CodegenVisitor.flags);

        String qualifiedName = FilenameUtils.removeExtension(source.getFullRelativePath());
        cw.visit(CodegenVisitor.CLASS_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, qualifiedName, null, "java/lang/Object", null);

        var initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, CodegenVisitor.Init, "()V", null, null);
        var ga = new GeneratorAdapter(initMv, Opcodes.ACC_PUBLIC, CodegenVisitor.Init, "()V");
        ga.visitVarInsn(Opcodes.ALOAD, 0);
        ga.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                CodegenVisitor.Init,
                "()V",
                false
        );
        ga.returnValue();
        ga.endMethod();

        cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "instance", "L" + qualifiedName + ";", null, null);

        var mvStatic = cw.visitMethod(Opcodes.ACC_STATIC, CodegenVisitor.Clinit, "()V", null, null);
        ga = new GeneratorAdapter(mvStatic, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, CodegenVisitor.Clinit, "()V");
        var t = Type.getType("L" + qualifiedName + ";");
        ga.newInstance(t);
        ga.dup();
        ga.invokeConstructor(t, new Method(CodegenVisitor.Init, "()V"));

        mvStatic.visitFieldInsn(Opcodes.PUTSTATIC, qualifiedName, "instance", "L" + qualifiedName + ";");
        mvStatic.visitInsn(Opcodes.RETURN);
        ga.endMethod();

        var codegenVisitor = new CodegenVisitor(ixApi, source.rootContext, source, cw);

        source.acceptVisitor(codegenVisitor);

        cw.visitEnd();

        return new Pair<>(cw, codegenVisitor.structWriters);
    }

}
