package com.kingmang.ixion.types;

import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.util.Pair;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;


public class TypeUtil {

	public static void generateCorrectInt(int val, Context context) {
		MethodVisitor method = context.getMethodVisitor();

		if(val >= 0) {
			if(val <= 5) {
				method.visitInsn(switch(val) {
					case 0 -> Opcodes.ICONST_0;
					case 1 -> Opcodes.ICONST_1;
					case 2 -> Opcodes.ICONST_2;
					case 3 -> Opcodes.ICONST_3;
					case 4 -> Opcodes.ICONST_4;
					case 5 -> Opcodes.ICONST_5;
					default -> 0;
				});
			}
			else if(val < 128) method.visitIntInsn(Opcodes.BIPUSH, val);
			else if(val < 32768) method.visitIntInsn(Opcodes.SIPUSH, val);
			else method.visitLdcInsn(val);
		}
		else {
			if(val == -1) method.visitInsn(Opcodes.ICONST_M1);
			else if(val >= -128) method.visitIntInsn(Opcodes.BIPUSH, val);
			else if(val >= -32768) method.visitIntInsn(Opcodes.SIPUSH, val);
			else method.visitLdcInsn(val);
		}
	}

	public static void generateCorrectDouble(double val, Context context) {
		MethodVisitor method = context.getMethodVisitor();

		if (val == 0) method.visitInsn(Opcodes.DCONST_0);
		else if (val == 1) method.visitInsn(Opcodes.DCONST_1);
		else method.visitLdcInsn(val);
	}


	public static void generateCorrectFloat(float val, Context context) {
		MethodVisitor method = context.getMethodVisitor();

		if (val == 0) method.visitInsn(Opcodes.FCONST_0);
		else if (val == 1) method.visitInsn(Opcodes.FCONST_1);
		else if (val == 2) method.visitInsn(Opcodes.FCONST_2);
		else method.visitLdcInsn(val);
	}


	public static void generateCorrectLong(long val, Context context) {
		MethodVisitor method = context.getMethodVisitor();

		if (val == 0) method.visitInsn(Opcodes.LCONST_0);
		else if (val == 1) method.visitInsn(Opcodes.LCONST_1);
		else method.visitLdcInsn(val);
	}

	public static void correctLdc(Object object, Context context) {
        switch (object) {
            case null -> context.getMethodVisitor().visitInsn(Opcodes.ACONST_NULL);
            case Integer i -> generateCorrectInt(i, context);
            case Double v -> generateCorrectDouble(v, context);
            case Float v -> generateCorrectFloat(v, context);
            case Boolean b -> context.getMethodVisitor().visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            case Character c -> generateCorrectInt(c, context);
            case Long l -> generateCorrectLong(l, context);
            case Byte b -> generateCorrectInt(b, context);
            case Short i -> generateCorrectInt(i, context);
            default -> context.getMethodVisitor().visitLdcInsn(object);
        }
	}

	public static void insertInvoke(FileContext context, Method method, boolean superCall) {
		Class<?> klass = method.getDeclaringClass();
		boolean isInterface = Modifier.isInterface(klass.getModifiers());
		context.getContext().getMethodVisitor().visitMethodInsn(superCall ? Opcodes.INVOKESPECIAL : isInterface ? Opcodes.INVOKEINTERFACE : TypeUtil.getInvokeOpcode(method), klass.getName().replace('.', '/'), method.getName(), "(%s)%s".formatted(Arrays.stream(IxType.getType(method).getArgumentTypes()).map(IxType::getDescriptor).collect(Collectors.joining()), Type.getType(method.getReturnType()).getDescriptor()), isInterface);
	}

	public static int getInvokeOpcode(Method m) {
		return Modifier.isStatic(m.getModifiers()) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
	}

	public static int getAccessOpcode(Field f) {
		return Modifier.isStatic(f.getModifiers()) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
	}


	public static int getMemberPutOpcode(Field f) {
		return Modifier.isStatic(f.getModifiers()) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
	}


	public static Class<?> classForName(String name, Context context) throws ClassNotFoundException {
		String className = name;

		if(context.getUsings().get(name) != null) className = context.getUsings().get(name);

		return Class.forName(className, false, context.getLoader());
	}

	public static Constructor<?> getConstructor(Token location, Constructor<?>[] constructors, IxType[] argTypes, Context context) throws IxException {

		ArrayList<Pair<Integer, Constructor<?>>> possible = new ArrayList<>();

		try {
			out:
			for (Constructor<?> c : constructors) {
				IxType[] expectArgs = IxType.getType(c).getArgumentTypes();

				if (expectArgs.length != argTypes.length) continue;

				int changes = 0;

				for (int i = 0; i < expectArgs.length; i++) {
					IxType expectArg = expectArgs[i];
					IxType arg = argTypes[i];

					if (arg.equals(IxType.VOID_TYPE))
						continue out;

					if (expectArg.isAssignableFrom(arg, context, false)) {
						if (!expectArg.equals(arg)) changes += expectArg.assignChangesFrom(arg);
					} else {
						continue out;
					}
				}
				possible.add(new Pair<>(changes, c));
			}
		}
		catch(ClassNotFoundException e) {
			throw new IxException(location, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		if(possible.isEmpty()) return null;

		possible.sort(Comparator.comparingInt(Pair::first));

		return possible.getFirst().second();
	}

}
