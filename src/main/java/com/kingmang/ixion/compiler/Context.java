package com.kingmang.ixion.compiler;
import com.kingmang.ixion.ast.AnnotationNode;
import com.kingmang.ixion.class_utils.CustomClassLoader;
import com.kingmang.ixion.ast.VariableDeclarationNode;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Context {
	@Getter
    private final HashMap<String, String> usings;

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
	private Label loopStartLabel;
	@Setter
	@Getter
	private Label loopEndLabel;

	@Getter
    private List<AnnotationNode> annotations = new ArrayList<>();

	public Context() {
		this.usings = new HashMap<>();
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
		usings.put("Appendable", "java.lang.Appendable");
		usings.put("AutoCloseable", "java.lang.AutoCloseable");
		usings.put("Monad", "com.kingmang.ixion.monad.Monad");
		usings.put("Maybe", "com.kingmang.ixion.monad.Maybe");
		usings.put("CharSequence", "java.lang.CharSequence");
		usings.put("Cloneable", "java.lang.Cloneable");
		usings.put("Comparable", "java.lang.Comparable");
		usings.put("Iterable", "java.lang.Iterable");
		usings.put("Readable", "java.lang.Readable");
		usings.put("Runnable", "java.lang.Runnable");
		usings.put("Boolean", "java.lang.Boolean");
		usings.put("Byte", "java.lang.Byte");
		usings.put("Character", "java.lang.Character");
		usings.put("Class", "java.lang.Class");
		usings.put("CustomClassLoader", "java.lang.CustomClassLoader");
		usings.put("ClassValue", "java.lang.ClassValue");
		usings.put("Compiler", "java.lang.Compiler");
		usings.put("Double", "java.lang.Double");
		usings.put("Enum", "java.lang.Enum");
		usings.put("Float", "java.lang.Float");
		usings.put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
		usings.put("Integer", "java.lang.Integer");
		usings.put("Long", "java.lang.Long");
		usings.put("Math", "java.lang.Math");
		usings.put("Number", "java.lang.Number");
		usings.put("Object", "java.lang.Object");
		usings.put("Package", "java.lang.Package");
		usings.put("Process", "java.lang.Process");
		usings.put("ProcessBuilder", "java.lang.ProcessBuilder");
		usings.put("Runtime", "java.lang.Runtime");
		usings.put("RuntimePermission", "java.lang.RuntimePermission");
		usings.put("SecurityManager", "java.lang.SecurityManager");
		usings.put("Short", "java.lang.Short");
		usings.put("StackTraceElement", "java.lang.StackTraceElement");
		usings.put("StrictMath", "java.lang.StrictMath");
		usings.put("String", "java.lang.String");
		usings.put("StringBuffer", "java.lang.StringBuffer");
		usings.put("StringBuilder", "java.lang.StringBuilder");
		usings.put("System", "java.lang.System");
		usings.put("Thread", "java.lang.Thread");
		usings.put("ThreadGroup", "java.lang.ThreadGroup");
		usings.put("ThreadLocal", "java.lang.ThreadLocal");
		usings.put("Throwable", "java.lang.Throwable");
		usings.put("Void", "java.lang.Void");
		usings.put("ArithmeticException", "java.lang.ArithmeticException");
		usings.put("ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException");
		usings.put("ArrayStoreException", "java.lang.ArrayStoreException");
		usings.put("ClassCastException", "java.lang.ClassCastException");
		usings.put("ClassNotFoundException", "java.lang.ClassNotFoundException");
		usings.put("CloneNotSupportedException", "java.lang.CloneNotSupportedException");
		usings.put("EnumConstantNotPresentException", "java.lang.EnumConstantNotPresentException");
		usings.put("Exception", "java.lang.Exception");
		usings.put("IllegalAccessException", "java.lang.IllegalAccessException");
		usings.put("IllegalArgumentException", "java.lang.IllegalArgumentException");
		usings.put("IllegalMonitorStateException", "java.lang.IllegalMonitorStateException");
		usings.put("IllegalStateException", "java.lang.IllegalStateException");
		usings.put("IllegalThreadStateException", "java.lang.IllegalThreadStateException");
		usings.put("IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
		usings.put("InstantiationException", "java.lang.InstantiationException");
		usings.put("InterruptedException", "java.lang.InterruptedException");
		usings.put("NegativeArraySizeException", "java.lang.NegativeArraySizeException");
		usings.put("NoSuchFieldException", "java.lang.NoSuchFieldException");
		usings.put("NoSuchMethodException", "java.lang.NoSuchMethodException");
		usings.put("NullPointerException", "java.lang.NullPointerException");
		usings.put("NumberFormatException", "java.lang.NumberFormatException");
		usings.put("ReflectiveOperationException", "java.lang.ReflectiveOperationException");
		usings.put("RuntimeException", "java.lang.RuntimeException");
		usings.put("SecurityException", "java.lang.SecurityException");
		usings.put("StringIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException");
		usings.put("TypeNotPresentException", "java.lang.TypeNotPresentException");
		usings.put("UnsupportedOperationException", "java.lang.UnsupportedOperationException");
		usings.put("AbstractMethodError", "java.lang.AbstractMethodError");
		usings.put("AssertionError", "java.lang.AssertionError");
		usings.put("BootstrapMethodError", "java.lang.BootstrapMethodError");
		usings.put("ClassCircularityError", "java.lang.ClassCircularityError");
		usings.put("ClassFormatError", "java.lang.ClassFormatError");
		usings.put("Error", "java.lang.Error");
		usings.put("ExceptionInInitializerError", "java.lang.ExceptionInInitializerError");
		usings.put("IllegalAccessError", "java.lang.IllegalAccessError");
		usings.put("IncompatibleClassChangeError", "java.lang.IncompatibleClassChangeError");
		usings.put("InstantiationError", "java.lang.InstantiationError");
		usings.put("InternalError", "java.lang.InternalError");
		usings.put("LinkageError", "java.lang.LinkageError");
		usings.put("NoClassDefFoundError", "java.lang.NoClassDefFoundError");
		usings.put("NoSuchFieldError", "java.lang.NoSuchFieldError");
		usings.put("NoSuchMethodError", "java.lang.NoSuchMethodError");
		usings.put("OutOfMemoryError", "java.lang.OutOfMemoryError");
		usings.put("StackOverflowError", "java.lang.StackOverflowError");
		usings.put("ThreadDeath", "java.lang.ThreadDeath");
		usings.put("UnknownError", "java.lang.UnknownError");
		usings.put("UnsatisfiedLinkError", "java.lang.UnsatisfiedLinkError");
		usings.put("UnsupportedClassVersionError", "java.lang.UnsupportedClassVersionError");
		usings.put("VerifyError", "java.lang.VerifyError");
		usings.put("VirtualMachineError", "java.lang.VirtualMachineError");
		usings.put("Deprecated", "java.lang.Deprecated");
		usings.put("Override", "java.lang.Override");
		usings.put("SafeVarargs", "java.lang.SafeVarargs");
		usings.put("SuppressWarnings", "java.lang.SuppressWarnings");
	}

	public void addAnnotation(AnnotationNode annotation) {
		annotations.add(annotation);
	}

    public void clearAnnotations() {
		annotations.clear();
	}
}
