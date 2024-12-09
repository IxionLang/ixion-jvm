package com.kingmang.ixion.compiler;

import com.kingmang.ixion.class_utils.CustomClassLoader;
import com.kingmang.ixion.ast.VariableDeclarationNode;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Context {
	@Getter
    private final HashMap<String, String> imports;

	@Getter
    private final Map<String, ClassWriter> classWriterMap;

	@Setter
    @Getter
    private ContextType type;

	@Setter
    @Getter
    private String source;

	@Setter
    @Getter
    private String packageName;

	@Setter
    @Getter
    private String currentClass;

	@Setter
    @Getter
    private MethodVisitor methodVisitor;

	@Setter
    @Getter
    private MethodVisitor staticMethodVisitor;

	@Setter
    @Getter
    private MethodVisitor defaultConstructor;

	@Setter
    @Getter
    private CustomClassLoader loader;

	@Setter
    @Getter
    private Scope scope;

	@Setter
    @Getter
    private IxType currentSuperClass;
	private boolean isStaticMethod;
	@Setter
    @Getter
    private boolean constructor;
	private int currentLine;

	@Setter
    @Getter
    private List<VariableDeclarationNode> classVariables;

	@Setter
    private Label nullJumpLabel;

	@Setter
	@Getter
	private Label cycleStartLabel;
	@Setter
	@Getter
	private Label cycleEndLabel;

	public Context() {
		this.imports = new HashMap<>();
		this.classWriterMap = new HashMap<>();
		initImports();
	}

    public boolean isStaticMethod() {
		return isStaticMethod;
	}

	public void setStaticMethod(boolean staticMethod) {
		isStaticMethod = staticMethod;
	}

    public void updateLine(int line) {
		if(currentLine == line) return;
		currentLine = line;
		Label l = new Label();
		MethodVisitor mv = type == ContextType.GLOBAL ? getStaticMethodVisitor() : getMethodVisitor();
		if(mv == null) mv = getDefaultConstructor();
		mv.visitLabel(l);
		mv.visitLineNumber(line, l);
	}

	public ClassWriter getCurrentClassWriter() {
		return classWriterMap.get(currentClass);
	}

	public Label getNullJumpLabel(Label other) {
		return nullJumpLabel == null ? other : nullJumpLabel;
	}

    private void initImports() {
		imports.put("Appendable", "java.lang.Appendable");
		imports.put("AutoCloseable", "java.lang.AutoCloseable");
		imports.put("Monad", "com.kingmang.ixion.monad.Monad");
		imports.put("Maybe", "com.kingmang.ixion.monad.Maybe");
		imports.put("CharSequence", "java.lang.CharSequence");
		imports.put("Cloneable", "java.lang.Cloneable");
		imports.put("Comparable", "java.lang.Comparable");
		imports.put("Iterable", "java.lang.Iterable");
		imports.put("Readable", "java.lang.Readable");
		imports.put("Runnable", "java.lang.Runnable");
		imports.put("Boolean", "java.lang.Boolean");
		imports.put("Byte", "java.lang.Byte");
		imports.put("Character", "java.lang.Character");
		imports.put("Class", "java.lang.Class");
		imports.put("CustomClassLoader", "java.lang.CustomClassLoader");
		imports.put("ClassValue", "java.lang.ClassValue");
		imports.put("Compiler", "java.lang.Compiler");
		imports.put("Double", "java.lang.Double");
		imports.put("Enum", "java.lang.Enum");
		imports.put("Float", "java.lang.Float");
		imports.put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
		imports.put("Integer", "java.lang.Integer");
		imports.put("Long", "java.lang.Long");
		imports.put("Math", "java.lang.Math");
		imports.put("Number", "java.lang.Number");
		imports.put("Object", "java.lang.Object");
		imports.put("Package", "java.lang.Package");
		imports.put("Process", "java.lang.Process");
		imports.put("ProcessBuilder", "java.lang.ProcessBuilder");
		imports.put("Runtime", "java.lang.Runtime");
		imports.put("RuntimePermission", "java.lang.RuntimePermission");
		imports.put("SecurityManager", "java.lang.SecurityManager");
		imports.put("Short", "java.lang.Short");
		imports.put("StackTraceElement", "java.lang.StackTraceElement");
		imports.put("StrictMath", "java.lang.StrictMath");
		imports.put("String", "java.lang.String");
		imports.put("StringBuffer", "java.lang.StringBuffer");
		imports.put("StringBuilder", "java.lang.StringBuilder");
		imports.put("System", "java.lang.System");
		imports.put("Thread", "java.lang.Thread");
		imports.put("ThreadGroup", "java.lang.ThreadGroup");
		imports.put("ThreadLocal", "java.lang.ThreadLocal");
		imports.put("Throwable", "java.lang.Throwable");
		imports.put("Void", "java.lang.Void");
		imports.put("ArithmeticException", "java.lang.ArithmeticException");
		imports.put("ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException");
		imports.put("ArrayStoreException", "java.lang.ArrayStoreException");
		imports.put("ClassCastException", "java.lang.ClassCastException");
		imports.put("ClassNotFoundException", "java.lang.ClassNotFoundException");
		imports.put("CloneNotSupportedException", "java.lang.CloneNotSupportedException");
		imports.put("EnumConstantNotPresentException", "java.lang.EnumConstantNotPresentException");
		imports.put("Exception", "java.lang.Exception");
		imports.put("IllegalAccessException", "java.lang.IllegalAccessException");
		imports.put("IllegalArgumentException", "java.lang.IllegalArgumentException");
		imports.put("IllegalMonitorStateException", "java.lang.IllegalMonitorStateException");
		imports.put("IllegalStateException", "java.lang.IllegalStateException");
		imports.put("IllegalThreadStateException", "java.lang.IllegalThreadStateException");
		imports.put("IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
		imports.put("InstantiationException", "java.lang.InstantiationException");
		imports.put("InterruptedException", "java.lang.InterruptedException");
		imports.put("NegativeArraySizeException", "java.lang.NegativeArraySizeException");
		imports.put("NoSuchFieldException", "java.lang.NoSuchFieldException");
		imports.put("NoSuchMethodException", "java.lang.NoSuchMethodException");
		imports.put("NullPointerException", "java.lang.NullPointerException");
		imports.put("NumberFormatException", "java.lang.NumberFormatException");
		imports.put("ReflectiveOperationException", "java.lang.ReflectiveOperationException");
		imports.put("RuntimeException", "java.lang.RuntimeException");
		imports.put("SecurityException", "java.lang.SecurityException");
		imports.put("StringIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException");
		imports.put("TypeNotPresentException", "java.lang.TypeNotPresentException");
		imports.put("UnsupportedOperationException", "java.lang.UnsupportedOperationException");
		imports.put("AbstractMethodError", "java.lang.AbstractMethodError");
		imports.put("AssertionError", "java.lang.AssertionError");
		imports.put("BootstrapMethodError", "java.lang.BootstrapMethodError");
		imports.put("ClassCircularityError", "java.lang.ClassCircularityError");
		imports.put("ClassFormatError", "java.lang.ClassFormatError");
		imports.put("Error", "java.lang.Error");
		imports.put("ExceptionInInitializerError", "java.lang.ExceptionInInitializerError");
		imports.put("IllegalAccessError", "java.lang.IllegalAccessError");
		imports.put("IncompatibleClassChangeError", "java.lang.IncompatibleClassChangeError");
		imports.put("InstantiationError", "java.lang.InstantiationError");
		imports.put("InternalError", "java.lang.InternalError");
		imports.put("LinkageError", "java.lang.LinkageError");
		imports.put("NoClassDefFoundError", "java.lang.NoClassDefFoundError");
		imports.put("NoSuchFieldError", "java.lang.NoSuchFieldError");
		imports.put("NoSuchMethodError", "java.lang.NoSuchMethodError");
		imports.put("OutOfMemoryError", "java.lang.OutOfMemoryError");
		imports.put("StackOverflowError", "java.lang.StackOverflowError");
		imports.put("ThreadDeath", "java.lang.ThreadDeath");
		imports.put("UnknownError", "java.lang.UnknownError");
		imports.put("UnsatisfiedLinkError", "java.lang.UnsatisfiedLinkError");
		imports.put("UnsupportedClassVersionError", "java.lang.UnsupportedClassVersionError");
		imports.put("VerifyError", "java.lang.VerifyError");
		imports.put("VirtualMachineError", "java.lang.VirtualMachineError");
		imports.put("Deprecated", "java.lang.Deprecated");
		imports.put("Override", "java.lang.Override");
		imports.put("SafeVarargs", "java.lang.SafeVarargs");
		imports.put("SuppressWarnings", "java.lang.SuppressWarnings");
	}
}
