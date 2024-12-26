package com.kingmang.ixion.ast;

import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.ContextType;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.class_utils.CustomClassWriter;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassDeclarationNode implements Node {

	private final Token name;
	private final Node superclass;
	private final List<Node> declarations;
	private final List<ThisMethodDeclarationNode> constructors;
	private final boolean isFinal;
	private final Token access;
	private boolean staticVariableInit;

	public ClassDeclarationNode(Token name,
								Node superclass,
								List<Node> declarations,
								Token access,
								boolean isFinal) {
		this.name = name;
		this.superclass = superclass;
		this.declarations = declarations;
		this.constructors = new ArrayList<>();
		this.access = access;
		this.staticVariableInit = false;
		this.isFinal = isFinal;
	}

	@Override
	public void buildClasses(Context context) {
		ContextType prevType = context.getType();
		String prevClass = context.getCurrentClass();

		ClassWriter writer = initClass("java/lang/Object", context);

		writer.visitEnd();

		context.setType(prevType);
		context.setCurrentClass(prevClass);
	}

	@Override
	public void visit(FileContext fc) throws IxException {
		Context context = fc.getContext();
		ContextType prevType = context.getType();
		String prevClass = context.getCurrentClass();
		IxType prevSuperClass = context.getCurrentSuperClass();
		context.setCurrentSuperClass(getSuperclassType(context));

		ClassWriter writer = initClass(getSuperclassType(context).getInternalName(), context);

		MethodVisitor defaultConstructor = null;
		if(constructors.isEmpty()) {
			defaultConstructor = createDefaultConstructor(writer, context, true);

			context.setDefaultConstructor(defaultConstructor);
		}

		MethodVisitor staticMethod = null;
		if(staticVariableInit) {
			staticMethod = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			context.setStaticMethodVisitor(staticMethod);

			staticMethod.visitCode();
		}

		Scope outer = context.getScope();
		context.setScope(outer.nextDepth());
		context.getScope().updateCurrentClassMethods(fc);

		context.setMethodVisitor(null);
		for(Node declaration : declarations) {
			declaration.visit(fc);
		}

		for(ThisMethodDeclarationNode constructor : constructors) {
			constructor.visit(fc);
		}

		if(defaultConstructor != null) {
			defaultConstructor.visitInsn(Opcodes.RETURN);
			defaultConstructor.visitMaxs(0, 0);
			defaultConstructor.visitEnd();
		}

		if(staticVariableInit) {
            staticMethod.visitInsn(Opcodes.RETURN);
			staticMethod.visitMaxs(0, 0);
			staticMethod.visitEnd();
		}

		writer.visitEnd();

		context.setScope(outer);
		context.setType(prevType);
		context.setCurrentClass(prevClass);
		context.setCurrentSuperClass(prevSuperClass);
	}

	@Override
	public void preprocess(Context context) throws IxException {
		if(superclass != null) {
			IxType superclassType = superclass.getReturnType(context);
			if(superclassType.isPrimitive()) {
				throw new IxException(name, "Superclass must not be a primitive (got '%s').".formatted(superclassType));
			}
		}

		ContextType prevType = context.getType();
		String prevClass = context.getCurrentClass();
		IxType prevSuperClass = context.getCurrentSuperClass();
		context.setCurrentSuperClass(getSuperclassType(context));

		ClassWriter writer = initClass(getSuperclassType(context).getInternalName(), context);

		MethodVisitor defaultConstructor = null;

		Scope outer = context.getScope();
		context.setScope(outer.nextDepth());

		for(int i = 0; i < declarations.size(); i++) {
			Node declaration = declarations.get(i);
			if(declaration instanceof ThisMethodDeclarationNode) {
				constructors.add((ThisMethodDeclarationNode) declaration);
				declarations.remove(i);
				declaration.preprocess(context);
			}
		}

		if(constructors.isEmpty()) {
			defaultConstructor = createDefaultConstructor(writer, context, false);

			context.setDefaultConstructor(defaultConstructor);
		}


		context.setClassVariables(new ArrayList<>());
		for (Node declaration : declarations) {
			declaration.preprocess(context);
			if (declaration instanceof VariableDeclarationNode && ((VariableDeclarationNode) declaration).isStatic(context))
				staticVariableInit = true;
		}
		constructors.forEach(c -> c.setVariablesInit(context.getClassVariables()));
		context.setClassVariables(null);

		if(defaultConstructor != null) {
			defaultConstructor.visitInsn(Opcodes.RETURN);
			defaultConstructor.visitMaxs(0, 0);
			defaultConstructor.visitEnd();
		}

		if(staticVariableInit) {
			MethodVisitor staticMethod = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			staticMethod.visitCode();
			staticMethod.visitInsn(Opcodes.RETURN);
			staticMethod.visitMaxs(0, 0);
			staticMethod.visitEnd();
		}

		writer.visitEnd();

		context.setScope(outer);
		context.setType(prevType);
		context.setCurrentClass(prevClass);
		context.setCurrentSuperClass(prevSuperClass);
	}

	private ClassWriter initClass(String superclassName, Context context) {
		context.setType(ContextType.CLASS);
		int accessLevel = getAccessLevel();

		String baseName = name.value();
		String className = baseName;

		if(accessLevel == 0) {
			className = context.getCurrentClass() + "PRIV" + baseName;
		}
		if(!context.getPackageName().isEmpty()) {
			className = context.getPackageName() + "/" + className;
		}
		if(!className.equals(baseName)) {
			context.getUsings().put(baseName, className.replace('/', '.'));
		}

		context.setCurrentClass(className);

		ClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, context.getLoader());

		if(!isFinal) {
			writer.visit(
					Opcodes.V9,
					accessLevel | Opcodes.ACC_SUPER, className,
					null, superclassName,
					null
			);
		}else{
			writer.visit(
					Opcodes.V9,
					accessLevel | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL, className,
					null, superclassName,
					null
			);
		}

		writer.visitSource(context.getSource(), null);

		context.getClassWriterMap().put(className, writer);

		return writer;
	}

	private MethodVisitor createDefaultConstructor(ClassWriter writer, Context context, boolean check) throws IxException {
		MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		constructor.visitCode();

		constructor.visitVarInsn(Opcodes.ALOAD, 0);
		constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, getSuperclassType(context).getInternalName(), "<init>", "()V", false);

		if(check) {
			boolean hasDefaultConstructor = false;
			try {
				Class<?> klass = Class.forName(getSuperclassType(context).getClassName(), false, context.getLoader());

				for(Constructor<?> superConstructor : klass.getConstructors()) {
					if(Modifier.isPrivate(superConstructor.getModifiers())) {
						continue;
					}
					if(superConstructor.getParameterTypes().length != 0) {
						continue;
					}
					hasDefaultConstructor = true;
				}
			} catch (ClassNotFoundException e) {
				throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
			if(!hasDefaultConstructor) {
				throw new IxException(name, "Cannot create default constructor: superclass '%s' has no default.".formatted(
						getSuperclassType(context)
				));
			}
		}

		return constructor;
	}

	private IxType getSuperclassType(Context context) throws IxException {
		if(superclass == null) return IxType.OBJECT_TYPE;
		return superclass.getReturnType(context);
	}

	private int getAccessLevel() {
		if(access == null || access.type() == TokenType.PUBLIC) return Opcodes.ACC_PUBLIC;
		return 0;
	}

	public String getName() {
		return name.value();
	}

	@Override
	public boolean isNewClass() {
		return true;
	}

	@Override
	public String toString() {
		return "class %s {%s}".formatted(name.value(), declarations.stream().map(Node::toString).collect(Collectors.joining()));
	}
}
