package com.kingmang.ixion.types;

import com.kingmang.ixion.compiler.Context;
import lombok.Getter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IxType {
	private static final List<Integer> TYPE_SIZE = List.of(Type.DOUBLE, Type.FLOAT, Type.LONG, Type.INT, Type.SHORT, Type.CHAR, Type.BYTE);
	public static IxType BOOLEAN_TYPE = new IxType(Type.BOOLEAN_TYPE);
	public static IxType BYTE_TYPE = new IxType(Type.BYTE_TYPE);
	public static IxType CHAR_TYPE = new IxType(Type.CHAR_TYPE);
	public static IxType DOUBLE_TYPE = new IxType(Type.DOUBLE_TYPE);
	public static IxType FLOAT_TYPE = new IxType(Type.FLOAT_TYPE);
	public static IxType INT_TYPE = new IxType(Type.INT_TYPE);
	public static IxType LONG_TYPE = new IxType(Type.LONG_TYPE);
	public static IxType SHORT_TYPE = new IxType(Type.SHORT_TYPE);
	public static IxType VOID_TYPE = new IxType(Type.VOID_TYPE);
	public static IxType NULL_TYPE = IxType.getObjectType("java/lang/Object");
	public static final IxType STRING_TYPE = IxType.getObjectType("java/lang/String");
	public static final IxType OBJECT_TYPE = IxType.getObjectType("java/lang/Object");
	public static final IxType NULLABLE_OBJECT_TYPE = OBJECT_TYPE.asNullable();

	static {
		NULL_TYPE.isNullable = true;
		NULL_TYPE.sort = Sort.NULL;
	}

	public enum Sort {
		VOID,
		BOOLEAN,
		CHAR,
		BYTE,
		SHORT,
		INT,
		FLOAT,
		LONG,
		DOUBLE,
		ARRAY,
		OBJECT,
		METHOD,
		NULL
	}


	private Type asmType;
	@Getter
    private Sort sort;
	private boolean isNullable;
	@Getter
    private IxType returnType;
	@Getter
    private IxType[] argumentTypes;
	@Getter
    private IxType elementType;
	private List<Integer> nullableDimensions;

	public IxType(Type asmType) {
		this.asmType = asmType;
		sort = Sort.values()[asmType.getSort()];
		this.isNullable = false;
		this.elementType = asmType.getSort() == Type.ARRAY ? new IxType(asmType.getElementType()) : null;
	}

	public IxType(Sort sort) {
		this.asmType = null;
		this.sort = sort;
		this.isNullable = false;
	}


	public boolean isPrimitive() {
		return  getSort() != Sort.OBJECT &&
				getSort() != Sort.ARRAY &&
				getSort() != Sort.METHOD &&
				getSort() != Sort.NULL;
	}

	public Class<?> toClass(Context context) throws ClassNotFoundException {
		return switch (asmType.getSort()) {
			case Type.BOOLEAN -> boolean.class;
			case Type.CHAR -> char.class;
			case Type.BYTE -> byte.class;
			case Type.SHORT -> short.class;
			case Type.INT -> int.class;
			case Type.FLOAT -> float.class;
			case Type.LONG -> long.class;
			case Type.DOUBLE -> double.class;
			case Type.ARRAY -> getElementType().toClass(context).arrayType();
			case Type.OBJECT -> Class.forName(getClassName(), false, context.getLoader());
			default -> throw new IllegalArgumentException("Unexpected value: " + asmType.getSort());
		};
	}

	public boolean isAssignableFrom(IxType from, Context context, boolean convert) throws ClassNotFoundException {

		if(isNullable() && (isArray() || isObject()) && from.isNull()) {
			return true;
		}
		if(isObject() && from.isObject()) {
			if(!isNullable() && from.isNullable()) return false;
			return toClass(context).isAssignableFrom(from.toClass(context));
		}

		else if(isPrimitive() && from.isPrimitive()) {
			if(TYPE_SIZE.indexOf(asmType.getSort()) <= TYPE_SIZE.indexOf(from.getRawType().getSort())) {
				if(convert) from.cast(this, context.getMethodVisitor());
				return true;
			}
			if(isRepresentedAsInteger() && from.isRepresentedAsInteger()) {
				if(convert) from.cast(this, context.getMethodVisitor());
				return true;
			}
			return false;
		}

		if(isArray() && from.isArray()) {
			if(!isNullable() && from.isNullable()) return false;
			if(getElementType().isNullable()) {
				return getElementType().equals(from.getElementType().asNullable());
			}
			return getElementType().equals(from.getElementType());
		}

		return false;
	}


	public int assignChangesFrom(IxType from) {
		if(from.equals(this) || from.isNull()) {
			return 0;
		}
		if(isObject() && from.isObject()) {
			return 1;
		}

		else if(isPrimitive() && from.isPrimitive()) {
			if(TYPE_SIZE.indexOf(getRawType().getSort()) <= TYPE_SIZE.indexOf(from.getRawType().getSort())) {
				return 1;
			}
			if(isRepresentedAsInteger() && from.isRepresentedAsInteger()) {
				return 2;
			}
		}

		if(isArray() && from.isArray()) {
			return 0;
		}

		return -1;
	}


	public IxType autoBox(MethodVisitor mv) {
		if(!isPrimitive()) return this;

		IxType wrapper = getAutoBoxWrapper();

		autoBox(mv, wrapper);

		return wrapper;
	}

	public IxType getAutoBoxWrapper() {
		if(!isPrimitive()) return this;

		String internalName = switch (asmType.getSort()) {
			case Type.BOOLEAN -> "java/lang/Boolean";
			case Type.BYTE -> "java/lang/Byte";
			case Type.CHAR -> "java/lang/Character";
			case Type.FLOAT -> "java/lang/Float";
			case Type.INT -> "java/lang/Integer";
			case Type.LONG -> "java/lang/Long";
			case Type.SHORT -> "java/lang/Short";
			case Type.DOUBLE -> "java/lang/Double";
			default -> null;
		};

		return IxType.getObjectType(internalName);
	}


	private void autoBox(MethodVisitor mv, IxType wrapper) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "valueOf", "(%s)%s".formatted(getDescriptor(), wrapper.getDescriptor()), false);
	}

	public int primitiveToTType() {
		if(!isPrimitive()) return -1;

		return switch (asmType.getSort()) {
			case Type.BOOLEAN -> Opcodes.T_BOOLEAN;
			case Type.CHAR -> Opcodes.T_CHAR;
			case Type.BYTE -> Opcodes.T_BYTE;
			case Type.SHORT -> Opcodes.T_SHORT;
			case Type.INT -> Opcodes.T_INT;
			case Type.LONG -> Opcodes.T_LONG;
			case Type.FLOAT -> Opcodes.T_FLOAT;
			case Type.DOUBLE -> Opcodes.T_DOUBLE;
			default -> 0;
		};
	}


	public int dummyConstant() {
		return switch (asmType.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> Opcodes.ICONST_0;
			case Type.LONG -> Opcodes.LCONST_0;
			case Type.FLOAT -> Opcodes.FCONST_0;
			case Type.DOUBLE -> Opcodes.DCONST_0;
			default -> Opcodes.ACONST_NULL;
		};
	}


	public void swap(IxType belowTop, MethodVisitor mv) {
		if (getSize() == 1) {
			if (belowTop.getSize() == 1) {
				mv.visitInsn(Opcodes.SWAP);
			} else {
				mv.visitInsn(Opcodes.DUP_X2);
				mv.visitInsn(Opcodes.POP);
			}
		} else {
			if (belowTop.getSize() == 1) {
				mv.visitInsn(Opcodes.DUP2_X1);
			} else {
				mv.visitInsn(Opcodes.DUP2_X2);
			}
			mv.visitInsn(Opcodes.POP2);
		}
	}


	public void cast(final IxType to, MethodVisitor mv) {
		if (!equals(to)) {
			if (equals(IxType.DOUBLE_TYPE)) {
				if (to.equals(IxType.FLOAT_TYPE)) {
					mv.visitInsn(Opcodes.D2F);
				} else if (to.equals(IxType.LONG_TYPE)) {
					mv.visitInsn(Opcodes.D2L);
				} else {
					mv.visitInsn(Opcodes.D2I);
					IxType.INT_TYPE.cast(to, mv);
				}
			} else if (equals(IxType.FLOAT_TYPE)) {
				if (to.equals(IxType.DOUBLE_TYPE)) {
					mv.visitInsn(Opcodes.F2D);
				} else if (to.equals(IxType.LONG_TYPE)) {
					mv.visitInsn(Opcodes.F2L);
				} else {
					mv.visitInsn(Opcodes.F2I);
					IxType.INT_TYPE.cast(to, mv);
				}
			} else if (equals(IxType.LONG_TYPE)) {
				if (to.equals(IxType.DOUBLE_TYPE)) {
					mv.visitInsn(Opcodes.L2D);
				} else if (to.equals(IxType.FLOAT_TYPE)) {
					mv.visitInsn(Opcodes.L2F);
				} else {
					mv.visitInsn(Opcodes.L2I);
					IxType.INT_TYPE.cast(to, mv);
				}
			} else {
				if (to.equals(IxType.BYTE_TYPE)) {
					mv.visitInsn(Opcodes.I2B);
				} else if (to.equals(IxType.CHAR_TYPE)) {
					mv.visitInsn(Opcodes.I2C);
				} else if (to.equals(IxType.DOUBLE_TYPE)) {
					mv.visitInsn(Opcodes.I2D);
				} else if (to.equals(IxType.FLOAT_TYPE)) {
					mv.visitInsn(Opcodes.I2F);
				} else if (to.equals(IxType.LONG_TYPE)) {
					mv.visitInsn(Opcodes.I2L);
				} else if (to.equals(IxType.SHORT_TYPE)) {
					mv.visitInsn(Opcodes.I2S);
				}
			}
		}
	}

	public int getPopOpcode() {
		if(getSize() == 2) return Opcodes.POP2;
		else if(getSize() == 1) return Opcodes.POP;
		return -1;
	}

	public int getDupOpcode() {
		if(getSize() == 2) return Opcodes.DUP2;
		else if(getSize() == 1) return Opcodes.DUP;
		return -1;
	}

	public int getDupX1Opcode() {
		return getSize() == 2 ? Opcodes.DUP2_X1 : Opcodes.DUP_X1;
	}

	public int getDupX2Opcode() {
		return getSize() == 2 ? Opcodes.DUP2_X2 : Opcodes.DUP_X2;
	}

	public boolean isInteger() {
		return isPrimitive() && (
						asmType.getSort() == Type.INT ||
						asmType.getSort() == Type.BYTE ||
						asmType.getSort() == Type.CHAR ||
						asmType.getSort() == Type.SHORT ||
						asmType.getSort() == Type.LONG ||
						asmType.getSort() == Type.BOOLEAN
		);
	}

	public boolean isRepresentedAsInteger() {
		return isPrimitive() && (
				asmType.getSort() == Type.INT ||
						asmType.getSort() == Type.BYTE ||
						asmType.getSort() == Type.CHAR ||
						asmType.getSort() == Type.SHORT ||
						asmType.getSort() == Type.BOOLEAN
		);
	}

	public boolean isFloat() {
		return asmType.getSort() == Type.DOUBLE ||
				asmType.getSort() == Type.FLOAT;
	}

	public boolean isNumeric() {
		return isInteger() || isFloat();
	}

	public void generateAsInteger(int value, Context context) {
		switch (sort) {
			case LONG -> TypeUtil.generateCorrectLong(value, context);
			case DOUBLE -> TypeUtil.generateCorrectDouble(value, context);
			case FLOAT -> TypeUtil.generateCorrectFloat(value, context);
			default -> TypeUtil.generateCorrectInt(value, context);
		}
	}

	public IxType getLarger(IxType right) {
		if(TYPE_SIZE.indexOf(asmType.getSort()) < TYPE_SIZE.indexOf(right.getRawType().getSort())) return this;
		else if(TYPE_SIZE.indexOf(asmType.getSort()) > TYPE_SIZE.indexOf(right.getRawType().getSort())) return right;
		return this;
	}

	public void compareInit(MethodVisitor mv) {
		switch (asmType.getSort()) {
			case Type.DOUBLE -> mv.visitInsn(Opcodes.DCMPL);
			case Type.FLOAT -> mv.visitInsn(Opcodes.FCMPL);
			case Type.LONG -> mv.visitInsn(Opcodes.LCMP);
		}
	}

	public IxType getRootElementType() {
		if(sort != Sort.ARRAY) {
			return this;
		}
		IxType element = getElementType();
		while(element.isArray()) {
			element = element.getElementType();
		}
		return element;
	}

	public boolean needsDimensionAnnotation() {
		return sort == Sort.ARRAY && nullableDimensions != null;
	}



	public IxType asNullable() {
		IxType type = copy();
		type.isNullable = true;
		return type;
	}

	public IxType asNullable(boolean isNullable) {
		IxType type = copy();
		type.isNullable = isNullable;
		return type;
	}

	public IxType asNonNullable() {
		IxType type = copy();
		type.isNullable = false;
		return type;
	}

	public boolean isObject() {
		return getSort() == Sort.OBJECT;
	}

	public boolean isArray() {
		return getSort() == Sort.ARRAY;
	}

	public boolean isNull() {
		return sort == Sort.NULL;
	}

	public String getInternalName() {
		return asmType.getInternalName();
	}

	public String getClassName() {
		return asmType.getClassName();
	}

    public int getOpcode(int opcode) {
		return asmType.getOpcode(opcode);
	}

	public int getSize() {
		return asmType.getSize();
	}

    public String getDescriptor() {
		if(sort == Sort.METHOD) {
			StringBuilder builder = new StringBuilder("(");
			for(IxType type : argumentTypes) {
				builder.append(type.getDescriptor());
			}
			return builder.append(")").append(returnType.getDescriptor()).toString();
		}
		return asmType.getDescriptor();
	}

    public boolean isNullable() {
		return isNullable;
	}

	private Type getRawType() {
		return asmType;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof IxType)) {
			return false;
		}
		IxType other = (IxType) obj;
		if(isNullable() != other.isNullable()) return false;
		return other.getRawType().equals(this.asmType);
	}

	@Override
	public int hashCode() {
		return asmType.hashCode();
	}

	public IxType copy() {
		IxType type = new IxType(sort);
		type.asmType = asmType;
		type.sort = sort;
		type.isNullable = isNullable;
		type.returnType = returnType;
		type.argumentTypes = argumentTypes;
		type.elementType = elementType;
		type.nullableDimensions = nullableDimensions;
		return type;
	}

	public String toString() {
		String base = switch (getSort()) {
			case VOID -> "void";
			case BOOLEAN -> "boolean";
			case CHAR -> "char";
			case BYTE -> "byte";
			case SHORT -> "short";
			case INT -> "int";
			case FLOAT -> "float";
			case LONG -> "long";
			case DOUBLE -> "double";
			case ARRAY -> getElementType() + "[]";
			case OBJECT -> asmType.getClassName();
			case METHOD -> "method";
			case NULL -> "null";
		};
		if(isNullable) base += "?";
		return base;
	}

	public static IxType getMethodType(String descriptor) {
		IxType type = new IxType(Type.getMethodType(descriptor));
		type.argumentTypes = Arrays.stream(type.getRawType().getArgumentTypes()).map(IxType::new).toArray(IxType[]::new);
		type.returnType = new IxType(type.getRawType().getReturnType());
		return type;
	}

	public static IxType getMethodType(IxType returnType, IxType... parameterTypes) {
		IxType type = new IxType(Sort.METHOD);
		type.returnType = returnType;
		type.argumentTypes = parameterTypes;
		return type;
	}

	public static IxType getArrayType(IxType elementType, int dimensions, List<Integer> nullableDimensions) {
		if(dimensions > 1) {
			elementType = getArrayType(elementType, dimensions - 1, nullableDimensions);
			if(nullableDimensions != null && nullableDimensions.contains(dimensions - 1)) elementType.isNullable = true;
		}
		IxType type = new IxType(Sort.ARRAY);
		type.elementType = elementType;
		type.asmType = Type.getType("[" + elementType.getRawType().getDescriptor());
		type.nullableDimensions = nullableDimensions;
		return type;
	}

	public static IxType getObjectType(String internalName) {
		return new IxType(Type.getObjectType(internalName));
	}

	public static IxType getType(String descriptor) {
		return new IxType(Type.getType(descriptor));
	}

	public static IxType getType(Class<?> clazz) {
		return new IxType(Type.getType(clazz));
	}

	private static IxType getArrayFromAnnotation(Class<?> typeClass) {
		int dim = 1;
		Class<?> elementClass = typeClass.getComponentType();

		while(elementClass.isArray()) {
			dim++;
			elementClass = elementClass.getComponentType();
		}
		IxType elementType = IxType.getType(elementClass);

		boolean isNullable = false;
		List<Integer> nullableDimensions = null;
		return IxType.getArrayType(elementType, dim, nullableDimensions).asNullable(isNullable);
	}

	public static IxType getType(Field field) {
		IxType type = IxType.getType(field.getType());

		Class<?> fieldClass = field.getType();
		if(fieldClass.isArray()) {
			type = getArrayFromAnnotation(fieldClass);
		}

		return type;
	}

	public static IxType getType(Method method) {
		Parameter[] parameters = method.getParameters();

		ArrayList<IxType> parameterTypes = new ArrayList<>();
		for(Parameter parameter : parameters) {
			Class<?> typeClass = parameter.getType();

			IxType type = IxType.getType(typeClass);

			if(typeClass.isArray()) {
				type = getArrayFromAnnotation(typeClass);
			}

			parameterTypes.add(type);
		}
		IxType returnType = IxType.getType(method.getReturnType());

		Class<?> returnClass = method.getReturnType();

		if(returnClass.isArray()) {
			returnType = getArrayFromAnnotation(returnClass);
		}

		return IxType.getMethodType(returnType, parameterTypes.toArray(IxType[]::new));
	}

	public static IxType getType(Constructor<?> constructor) {
		Parameter[] parameters = constructor.getParameters();

		ArrayList<IxType> parameterTypes = new ArrayList<>();
		for(Parameter parameter : parameters) {
			Class<?> typeClass = parameter.getType();

			IxType type = IxType.getType(typeClass);
			if(typeClass.isArray()) {
				type = getArrayFromAnnotation(typeClass);
			}

			parameterTypes.add(type);
		}

		return IxType.getMethodType(IxType.VOID_TYPE, parameterTypes.toArray(IxType[]::new));
	}
}
