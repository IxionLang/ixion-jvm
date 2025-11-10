package com.kingmang.ixion.runtime

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.GeneratorAdapter
import java.io.Serializable

enum class BuiltInType(
    name: String,
    typeClass: Class<*>,
    descriptor: String,
    opcodes: TypeSpecificOpcodes,
    defaultValue: Any?,
    isNumeric: Boolean
) : IxType, Serializable {
    BOOLEAN(
        "bool",
        Boolean::class.java,
        "Z",
        TypeSpecificOpcodes.INT,
        false,
        false
    ),

    CHAR(
        "char",
        Char::class.java,
        "C",
        TypeSpecificOpcodes.INT,
        '\u0000',
        true
    ),

    INT(
        "int",
        Int::class.java,
        "I",
        TypeSpecificOpcodes.INT,
        0,
        true
    ),

    FLOAT(
        "float",
        Float::class.java,
        "F",
        TypeSpecificOpcodes.FLOAT,
        0.0f,
        true
    ),

    DOUBLE(
        "double",
        Double::class.java,
        "D",
        TypeSpecificOpcodes.DOUBLE,
        0.0,
        true
    ),

    STRING(
        "string",
        String::class.java,
        "Ljava/lang/String;",
        TypeSpecificOpcodes.OBJECT,
        "",
        false
    ),

    VOID(
        "void",
        Void.TYPE,
        "V",
        TypeSpecificOpcodes.VOID,
        null,
        false
    ),

    ANY(
        "any",
        Any::class.java,
        "Ljava/lang/Object;",
        TypeSpecificOpcodes.OBJECT,
        false,
        false
    );

    private val namee: String?
    private val typeClass: Class<*>?
    private val descriptor: String?
    private val opcodes: TypeSpecificOpcodes
    private val defaultValue: Any?
    private val isNumeric: Boolean

    init {
        this.namee = name
        this.typeClass = typeClass
        this.descriptor = descriptor
        this.opcodes = opcodes
        this.defaultValue = defaultValue
        this.isNumeric = isNumeric
    }

    fun doBoxing(mv: MethodVisitor) {
        when (this) {
            BuiltInType.INT -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Integer",
                "valueOf",
                "(I)Ljava/lang/Integer;",
                false
            )

            BuiltInType.FLOAT -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Float",
                "valueOf",
                "(F)Ljava/lang/Float;",
                false
            )

            BuiltInType.DOUBLE -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Double",
                "valueOf",
                "(D)Ljava/lang/Double;",
                false
            )

            BuiltInType.BOOLEAN -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Boolean",
                "valueOf",
                "(Z)Ljava/lang/Boolean;",
                false
            )

            BuiltInType.STRING -> {}
            BuiltInType.ANY -> {}
            else -> System.err.println("Boxing isn't supported for that type.")
        }
    }

    fun doUnboxing(mv: MethodVisitor) {
        when (this) {
            BuiltInType.INT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
            }

            BuiltInType.CHAR -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character")
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false)
            }

            BuiltInType.FLOAT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "floatValue", "()F", false)
            }

            BuiltInType.DOUBLE -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false)
            }

            BuiltInType.BOOLEAN -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean")
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
            }

            BuiltInType.STRING -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            else -> {
                System.err.println("Unboxing isn't supported for that type.")
                System.exit(29)
            }
        }
    }

    val addOpcode: Int
        get() = opcodes.getAdd()

    override fun getDefaultValue(): Any? {
        return this.defaultValue
    }

    override fun getDescriptor(): String? {
        return descriptor
    }

    val divideOpcode: Int
        get() = opcodes.getDivide()

    override fun getInternalName(): String? {
        return getDescriptor()
    }

    override fun getLoadVariableOpcode(): Int {
        return opcodes.getLoad()
    }

    val multiplyOpcode: Int
        get() = opcodes.getMultiply()

    override fun getName(): String? {
        return name
    }

    val negOpcode: Int
        get() = opcodes.getNeg()

    override fun getReturnOpcode(): Int {
        return opcodes.getReturn()
    }

    val subtractOpcode: Int
        get() = opcodes.getSubtract()

    override fun getTypeClass(): Class<*>? {
        return typeClass
    }

    override fun isNumeric(): Boolean {
        return isNumeric
    }

    override fun kind(): String? {
        return null
    }

    fun pushOne(ga: GeneratorAdapter) {
        when (this) {
            BuiltInType.INT, BuiltInType.CHAR -> ga.push(1)
            BuiltInType.FLOAT -> ga.push(1.0f)
            BuiltInType.DOUBLE -> ga.push(1.0)
            else -> ga.push(0)
        }
    }

    override fun toString(): String {
        return getName()!!
    }

    fun unboxNoCheck(mv: MethodVisitor) {
        when (this) {
            BuiltInType.INT -> mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
            BuiltInType.CHAR -> mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Character",
                "charValue",
                "()C",
                false
            )

            BuiltInType.FLOAT -> mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Float",
                "floatValue",
                "()F",
                false
            )

            BuiltInType.DOUBLE -> mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Double",
                "doubleValue",
                "()D",
                false
            )

            BuiltInType.BOOLEAN -> mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Boolean",
                "booleanValue",
                "()Z",
                false
            )

            else -> {}
        }
    }

    companion object {
        val widenings: MutableMap<BuiltInType?, Int?> = HashMap<BuiltInType?, Int?>()

        init {
            widenings.put(BuiltInType.BOOLEAN, -1)
            widenings.put(BuiltInType.CHAR, 0)
            widenings.put(BuiltInType.INT, 0)
            widenings.put(BuiltInType.FLOAT, 1)
            widenings.put(BuiltInType.DOUBLE, 2)
            widenings.put(BuiltInType.STRING, 10)
        }

        @JvmStatic
        fun widen(a: BuiltInType?, b: BuiltInType?): BuiltInType? {
            val priorityA: Int? = widenings.get(a)
            val priorityB: Int? = widenings.get(b)

            if (priorityA == null && priorityB == null) {
                return a
            }
            if (priorityA == null) {
                return b
            }
            if (priorityB == null) {
                return a
            }

            if (priorityA > priorityB) {
                return a
            }
            return b
        }
    }
}
