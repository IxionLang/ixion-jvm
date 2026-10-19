package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes

enum class TypeSpecificOpcodes(
    val load: Int,
    val store: Int,
    val `return`: Int,
    val add: Int,
    val subtract: Int,
    val multiply: Int,
    val divide: Int,
    val neg: Int
) {
    INT(
        Opcodes.ILOAD,
        Opcodes.ISTORE,
        Opcodes.IRETURN,
        Opcodes.IADD,
        Opcodes.ISUB,
        Opcodes.IMUL,
        Opcodes.IDIV,
        Opcodes.INEG
    ),
    FLOAT(
        Opcodes.FLOAD,
        Opcodes.FSTORE,
        Opcodes.FRETURN,
        Opcodes.FADD,
        Opcodes.FSUB,
        Opcodes.FMUL,
        Opcodes.FDIV,
        Opcodes.FNEG
    ),
    DOUBLE(
        Opcodes.DLOAD,
        Opcodes.DSTORE,
        Opcodes.DRETURN,
        Opcodes.DADD,
        Opcodes.DSUB,
        Opcodes.DMUL,
        Opcodes.DDIV,
        Opcodes.DNEG
    ),
    VOID(Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.RETURN, 0, 0, 0, 0, 0),
    OBJECT(Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.ARETURN, 0, 0, 0, 0, 0)
}