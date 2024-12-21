package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.ContextType;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.class_utils.CustomClassWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.stream.Collectors;

public class EnumDeclarationNode implements Node {
	private final Token name;
	private final List<Token> fields;
	private final Token access;

	public EnumDeclarationNode(Token name, List<Token> fields, Token access) {
		this.name = name;
		this.fields = fields;
		this.access = access;
	}

	@Override
	public void visit(FileContext fc) throws IxException {
		Context context = fc.getContext();

		ContextType prevType = context.getType();
		String prevClass = context.getCurrentClass();

		ClassWriter writer = initEnumClass(context);
		context.setType(ContextType.CLASS);

		Type enumType = Type.getObjectType(context.getCurrentClass());

		MethodVisitor staticMethod = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		staticMethod.visitCode();

		context.setMethodVisitor(staticMethod);

		buildFields(writer, staticMethod, enumType, context);

		buildValues(writer, staticMethod, enumType, context);

		staticMethod.visitInsn(Opcodes.RETURN);
		staticMethod.visitMaxs(0, 0);
		staticMethod.visitEnd();

		buildConstructor(writer, context);
		buildValuesMethod(writer, enumType, context);
		buildValueOfMethod(writer, enumType, context);

		writer.visitEnd();
		context.setCurrentClass(prevClass);
		context.setType(prevType);
	}

	@Override
	public void preprocess(Context context) throws IxException {

		String prevClass = context.getCurrentClass();

		ClassWriter writer = initEnumClass(context);

		Type enumType = Type.getObjectType(context.getCurrentClass());

		{
			MethodVisitor staticMethod = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			staticMethod.visitCode();
			staticMethod.visitInsn(Opcodes.RETURN);
			staticMethod.visitMaxs(0, 0);
			staticMethod.visitEnd();
		}

		{
			MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
			constructor.visitCode();
			constructor.visitVarInsn(Opcodes.ALOAD, 0);
			constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "()V", false);
			constructor.visitInsn(Opcodes.RETURN);
			constructor.visitMaxs(0, 0);
			constructor.visitEnd();
		}

		{
			MethodVisitor values = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()[" + enumType.getDescriptor(), null, null);
			values.visitCode();
			values.visitInsn(Opcodes.ACONST_NULL);
			values.visitInsn(Opcodes.ARETURN);
			values.visitMaxs(0, 0);
			values.visitEnd();
		}
		{
			MethodVisitor valueOf = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)" + enumType.getDescriptor(), null, null);
			valueOf.visitCode();
			valueOf.visitInsn(Opcodes.ACONST_NULL);
			valueOf.visitInsn(Opcodes.ARETURN);
			valueOf.visitMaxs(0, 0);
			valueOf.visitEnd();
		}


		for(Token field : fields) {
			writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, field.value(), enumType.getDescriptor(), null, null);
		}
		writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, "$VALUES", "[" + enumType.getDescriptor(), null, null);

		writer.visitEnd();
		context.setCurrentClass(prevClass);

	}

	@Override
	public boolean isNewClass() {
		return true;
	}

	@Override
	public void buildClasses(Context context) {
		String prevClass = context.getCurrentClass();

		ClassWriter writer = initEnumClass(context);
		writer.visitEnd();

		context.setCurrentClass(prevClass);
	}

	private ClassWriter initEnumClass(Context context) {
		int accessLevel = getAccessLevel();

		String baseName = name.value();
		String className = baseName;

		if(accessLevel == 0) {
			className = context.getCurrentClass() + "$" + baseName;
		}
		if(!context.getPackageName().isEmpty()) {
			className = context.getPackageName() + "/" + className;
		}
		if(!className.equals(baseName)) {
			context.getUsings().put(baseName, className.replace('/', '.'));
		}

		context.setCurrentClass(className);

		ClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, context.getLoader());

		writer.visit(Opcodes.V9, accessLevel | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, className, "Ljava/lang/Enum<L%s;>;".formatted(className), "java/lang/Enum", null);

		writer.visitSource(context.getSource(), null);

		context.getClassWriterMap().put(className, writer);

		return writer;
	}

	private void buildFields(ClassWriter writer, MethodVisitor staticMethod, Type enumType, Context context) {
		int count = 0;
		for(Token field : fields) {
			writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, field.value(), enumType.getDescriptor(), null, null);

			context.updateLine(field.line());
			staticMethod.visitTypeInsn(Opcodes.NEW, enumType.getInternalName());
			staticMethod.visitInsn(Opcodes.DUP);
			staticMethod.visitLdcInsn(field.value());
			TypeUtil.generateCorrectInt(count, context);
			staticMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, enumType.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
			staticMethod.visitFieldInsn(Opcodes.PUTSTATIC, enumType.getInternalName(), field.value(), enumType.getDescriptor());
		}
	}

	private void buildValues(ClassWriter writer, MethodVisitor staticMethod, Type enumType, Context context) {
		writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, "$VALUES", "[" + enumType.getDescriptor(), null, null);

		TypeUtil.generateCorrectInt(fields.size(), context);
		staticMethod.visitTypeInsn(Opcodes.ANEWARRAY, enumType.getInternalName());
		for(int i = 0; i < fields.size(); i++) {
			Token field = fields.get(i);

			staticMethod.visitInsn(Opcodes.DUP);
			TypeUtil.generateCorrectInt(i, context);
			staticMethod.visitFieldInsn(Opcodes.GETSTATIC, enumType.getInternalName(), field.value(), enumType.getDescriptor());
			staticMethod.visitInsn(Opcodes.AASTORE);
		}
		staticMethod.visitFieldInsn(Opcodes.PUTSTATIC, enumType.getInternalName(), "$VALUES", "[" + enumType.getDescriptor());
	}

	private void buildConstructor(ClassWriter writer, Context context) {
		MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
		constructor.visitCode();
		context.setMethodVisitor(constructor);
		context.updateLine(name.line());
		constructor.visitVarInsn(Opcodes.ALOAD, 0);
		constructor.visitVarInsn(Opcodes.ALOAD, 1);
		constructor.visitVarInsn(Opcodes.ILOAD, 2);
		constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
		constructor.visitInsn(Opcodes.RETURN);
		constructor.visitMaxs(0, 0);
		constructor.visitEnd();
	}

	private void buildValuesMethod(ClassWriter writer, Type enumType, Context context) {
		MethodVisitor values = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()[" + enumType.getDescriptor(), null, null);
		values.visitCode();
		context.setMethodVisitor(values);
		context.updateLine(name.line());

		values.visitFieldInsn(Opcodes.GETSTATIC, enumType.getInternalName(), "$VALUES", "[" + enumType.getDescriptor());
		values.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "[" + enumType.getDescriptor(), "clone", "()Ljava/lang/Object;", false);
		values.visitTypeInsn(Opcodes.CHECKCAST, "[" + enumType.getDescriptor());

		values.visitInsn(Opcodes.ARETURN);
		values.visitMaxs(0, 0);
		values.visitEnd();
	}

	private void buildValueOfMethod(ClassWriter writer, Type enumType, Context context) {
		MethodVisitor valueOf = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)" + enumType.getDescriptor(), null, null);
		valueOf.visitCode();

		context.setMethodVisitor(valueOf);
		context.updateLine(name.line());

		valueOf.visitLdcInsn(enumType);
		valueOf.visitVarInsn(Opcodes.ALOAD, 0);
		valueOf.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
		valueOf.visitTypeInsn(Opcodes.CHECKCAST, enumType.getInternalName());

		valueOf.visitInsn(Opcodes.ARETURN);
		valueOf.visitMaxs(0, 0);
		valueOf.visitEnd();
	}

	private int getAccessLevel() {
		if(access == null || access.type() == TokenType.PUBLIC) return Opcodes.ACC_PUBLIC;
		return 0;
	}

	@Override
	public String toString() {
		return "enum %s {%s\n}".formatted(name.value(), fields.stream().map(Token::value).collect(Collectors.joining(", ")));
	}
}
