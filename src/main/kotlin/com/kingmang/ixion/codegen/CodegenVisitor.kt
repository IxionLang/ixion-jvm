package com.kingmang.ixion.codegen

import com.kingmang.ixion.Visitor
import com.kingmang.ixion.api.Context
import com.kingmang.ixion.api.IxApi
import com.kingmang.ixion.api.IxApi.Companion.exit
import com.kingmang.ixion.api.IxFile
import com.kingmang.ixion.api.IxionConstant.ArrayListType
import com.kingmang.ixion.api.IxionConstant.Init
import com.kingmang.ixion.api.IxionConstant.IteratorType
import com.kingmang.ixion.api.IxionConstant.ListWrapperType
import com.kingmang.ixion.api.IxionConstant.ObjectType
import com.kingmang.ixion.api.IxionConstant.PublicStatic
import com.kingmang.ixion.ast.*
import com.kingmang.ixion.exception.IdentifierNotFoundException
import com.kingmang.ixion.exception.ImplementationException
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.runtime.BuiltInType.Companion.getFromToken
import com.kingmang.ixion.runtime.BuiltInType.Companion.widenings
import com.kingmang.ixion.typechecker.TypeResolver
import com.kingmang.ixion.typechecker.Coercions
import com.kingmang.ixion.typechecker.TypeTags
import com.kingmang.ixion.typechecker.TagExpression
import com.kingmang.ixion.typechecker.SemanticTypes
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.NotImplementedException
import org.objectweb.asm.*
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import java.io.File
import java.util.*
import java.util.function.BiConsumer

class CodegenVisitor(val api: IxApi, val rootContext: Context?, val source: IxFile, cw: ClassWriter) : Visitor<Optional<ClassWriter>> {
    private val file: File
    private val cw: ClassWriter
    private val structWriters: MutableMap<StructType, ClassWriter> = HashMap()
    private val functionStack = Stack<DefType>()
    private var lambdaCounter = 0

    var currentContext: Context?

    init {
        this.currentContext = this.rootContext
        this.cw = cw
        this.file = source.file
    }

    fun getStructWriters(): Map<StructType, ClassWriter> {
        return this.structWriters
    }

    private fun specialized(type: IxType): IxType = type
    private fun tagArgument(function: DefType): Int = if (function.reifiedArgumentIndex >= 0) function.reifiedArgumentIndex else function.parameters.size
    private fun emitTag(tag: TagExpression) {
        val ga = functionStack.peek().ga!!
        when (tag) {
            is TagExpression.Text -> ga.push(tag.value)
            is TagExpression.Parameter -> {
                val function = functionStack.peek()
                val index = function.generics.indexOf(tag.name)
                require(index >= 0) { "Unbound runtime type parameter ${tag.name}" }
                ga.loadArg(tagArgument(function)); ga.push(index); ga.arrayLoad(Type.getType(String::class.java))
            }
            is TagExpression.Union -> {
                ga.push(tag.alternatives.size); ga.newArray(Type.getType(String::class.java))
                for ((index, part) in tag.alternatives.withIndex()) { ga.dup(); ga.push(index); emitTag(part); ga.arrayStore(Type.getType(String::class.java)) }
                ga.invokeStatic(Type.getType(RuntimeTypeTags::class.java), Method("union", "([Ljava/lang/String;)Ljava/lang/String;"))
            }
            is TagExpression.Join -> {
                val builder = Type.getType(StringBuilder::class.java)
                ga.newInstance(builder); ga.dup(); ga.invokeConstructor(builder, Method("<init>", "()V"))
                for (part in tag.parts) { emitTag(part); ga.invokeVirtual(builder, Method("append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")) }
                ga.invokeVirtual(builder, Method("toString", "()Ljava/lang/String;"))
            }
        }
    }
    private fun emitTags(types: List<IxType>) {
        val ga = functionStack.peek().ga!!
        ga.push(types.size); ga.newArray(Type.getType(String::class.java))
        for ((index, type) in types.withIndex()) { ga.dup(); ga.push(index); emitTag(TypeTags.expression(type)); ga.arrayStore(Type.getType(String::class.java)) }
    }
    private fun box(type: IxType) {
        val ga = functionStack.peek().ga!!
        if (type === BuiltInType.VOID) ga.getStatic(Type.getType(UnitValue::class.java), "INSTANCE", Type.getType(UnitValue::class.java))
        else if (type is BuiltInType) type.doBoxing(ga)
    }
    private fun fromObject(type: IxType) {
        val ga = functionStack.peek().ga!!
        when (type) {
            is GenericType -> Unit
            is BuiltInType -> if (type !== BuiltInType.ANY && type !== BuiltInType.VOID) type.doUnboxing(ga)
            else -> ga.checkCast(Type.getType(type.descriptor))
        }
    }
    private fun functionDescriptor(type: DefType): String {
        if (type.glue && type.jvmDescriptor != null) return type.jvmDescriptor!!
        val descriptor = CollectionUtil.getMethodDescriptor(type.parameters, type.returnType)
        return if (type.generics.isEmpty() || type.glue) descriptor else descriptor.replace(")", "[Ljava/lang/String;)")
    }
    private fun emitAdt(type: AdtType, variant: AdtVariant, args: List<Expression>) {
        val ga = functionStack.peek().ga!!
        val runtime = Type.getType(AdtValue::class.java)
        ga.newInstance(runtime); ga.dup(); emitTag(TypeTags.expression(type)); ga.push(variant.name)
        ga.push(args.size); ga.newArray(ObjectType)
        val fields = type.fields(variant)
        for (index in args.indices) {
            ga.dup(); ga.push(index); args[index].accept(this)
            coerce(args[index].realType, fields[index].second); box(fields[index].second); ga.arrayStore(ObjectType)
        }
        ga.invokeConstructor(runtime, Method("<init>", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V"))
    }

    /** Convert an already evaluated stack value to its expected language type. */
    private fun coerce(actual: IxType, expected: IxType) {
        val from = specialized(actual)
        val to = specialized(expected)
        val ga = functionStack.peek().ga!!
        if (to === BuiltInType.VOID || to === UnitType) {
            if (from !== BuiltInType.VOID && from !== NeverType && to === BuiltInType.VOID) ga.pop()
            return
        }
        if (to is GenericType) {
            box(from)
            return
        }
        if (to is UnionType) {
            if (from is UnionType) {
                val mapping = Coercions.unionMapping(to, from)
                if (mapping.isNotEmpty()) {
                    ga.push(mapping)
                    ga.invokeStatic(Type.getType(UnionCoercions::class.java),
                        Method("convert", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"))
                }
            } else {
                val alternative = Coercions.unionTarget(to, from)
                coerce(from, alternative)
                if (alternative is BuiltInType) alternative.doBoxing(ga)
            }
        } else if ((to === BuiltInType.ANY || to is ExternalType && to.foundClass == Any::class.java) && from is BuiltInType) {
            from.doBoxing(ga)
        } else if (to is BuiltInType && from is BuiltInType && to != from && to.isNumeric && from.isNumeric) {
            ga.cast(Type.getType(from.descriptor), Type.getType(to.descriptor))
        }
    }

    override fun visitUnit(expression: UnitExpression): Optional<ClassWriter> {
        functionStack.peek().ga!!.getStatic(Type.getType(UnitValue::class.java), "INSTANCE", Type.getType(UnitValue::class.java))
        return Optional.empty()
    }
    override fun visitTuple(expression: TupleExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        val type = expression.realType as TupleType
        val runtime = Type.getType(TupleValue::class.java)
        ga.newInstance(runtime); ga.dup(); emitTag(TypeTags.expression(type))
        ga.push(expression.elements.size); ga.newArray(ObjectType)
        for ((index, element) in expression.elements.withIndex()) {
            ga.dup(); ga.push(index); element.accept(this); box(element.realType); ga.arrayStore(ObjectType)
        }
        ga.invokeConstructor(runtime, Method("<init>", "(Ljava/lang/String;[Ljava/lang/Object;)V"))
        return Optional.empty()
    }
    override fun visitTypeApplication(expression: TypeApplicationExpression): Optional<ClassWriter> {
        val type = expression.realType as AdtType
        emitAdt(type, type.declaration.variants.first { it.name == expression.name.source.substringAfterLast("::") }, emptyList())
        return Optional.empty()
    }
    override fun visitUpdate(expression: UpdateExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        val type = expression.realType as StructType
        val raw = if (type is AppliedStructType) type.base else type
        expression.base.accept(this)
        val base = ga.newLocal(Type.getType(raw.descriptor)); ga.storeLocal(base)
        val updates = linkedMapOf<String, Int>()
        for ((name, value) in expression.fields) {
            val field = raw.parameters.first { it.first == name.source }
            val expected = type.parameters.first { it.first == name.source }.second
            value.accept(this); coerce(value.realType, expected)
            if (field.second is GenericType) box(expected)
            val local = ga.newLocal(Type.getType(field.second.descriptor)); ga.storeLocal(local); updates[name.source] = local
        }
        ga.newInstance(Type.getType(raw.descriptor)); ga.dup()
        for ((name, field) in raw.parameters) {
            val update = updates[name]
            if (update != null) ga.loadLocal(update) else {
                ga.loadLocal(base); ga.push(raw.parameters.indexOfFirst { it.first == name })
                ga.invokeInterface(Type.getType(IxDataValue::class.java), Method("ixField", "(I)Ljava/lang/Object;"))
                fromObject(field)
            }
        }
        if (raw.hasGenerics()) emitTag(TypeTags.expression(type))
        val descriptor = raw.parameters.joinToString("") { it.second.descriptor!! } + if (raw.hasGenerics()) "Ljava/lang/String;" else ""
        ga.invokeConstructor(Type.getType(raw.descriptor), Method("<init>", "($descriptor)V"))
        return Optional.empty()
    }
    override fun visitBlockExpression(expression: BlockExpression): Optional<ClassWriter> {
        val previous = currentContext
        currentContext = expression.block.context
        expression.block.accept(this)
        if (expression.realType !== NeverType) {
            if (expression.result == null) visitUnit(UnitExpression(expression.position!!)) else expression.result.accept(this)
        }
        currentContext = previous
        return Optional.empty()
    }
    override fun visitCaseExpression(expression: CaseExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        val function = functionStack.peek()
        expression.subject.accept(this); box(expression.subject.realType)
        val subject = ga.newLocal(ObjectType); ga.storeLocal(subject)
        val result = if (expression.realType === NeverType) -1 else ga.newLocal(Type.getType(expression.realType.descriptor))
        val end = Label()
        val previousContext = currentContext
        val previousLocals = HashMap(function.localMap)
        for (branch in expression.branches) {
            val next = Label()
            val plan = branch.plan!!
            ga.loadLocal(subject); ga.push(plan.encoded); emitTags(plan.tags)
            ga.invokeStatic(Type.getType(MatchRuntime::class.java), Method("matches", "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;)Z"))
            ga.ifZCmp(GeneratorAdapter.EQ, next)
            currentContext = branch.context
            for (binding in plan.bindings) {
                ga.loadLocal(subject); ga.push(binding.path)
                ga.invokeStatic(Type.getType(MatchRuntime::class.java), Method("extract", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"))
                fromObject(binding.type)
                val local = ga.newLocal(Type.getType(binding.type.descriptor)); ga.storeLocal(local); function.localMap[binding.name] = local
            }
            if (branch.guard != null) { branch.guard.accept(this); ga.ifZCmp(GeneratorAdapter.EQ, next) }
            branch.body.accept(this)
            if (branch.body.realType !== NeverType) {
                coerce(branch.body.realType, expression.realType); ga.storeLocal(result); ga.goTo(end)
            }
            ga.mark(next)
            function.localMap.clear(); function.localMap.putAll(previousLocals)
        }
        ga.throwException(Type.getType(IllegalStateException::class.java), "Unreachable: checked case has no matching branch")
        ga.mark(end)
        if (result >= 0) ga.loadLocal(result)
        currentContext = previousContext
        return Optional.empty()
    }

    override fun visit(statement: Statement): Optional<ClassWriter> {
        return statement.accept(this)
    }

    /**
     * Обрабатывает объявление псевдонима типа.
     * Поскольку псевдонимы не требуют генерации байт-кода, метод возвращает пустой Optional.
     * @param statement Выражение псевдонима типа
     * @return Пустой Optional
     */
    override fun visitTypeAlias(statement: TypeAliasStatement): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для операции присваивания значения переменной или полю объекта.
     * Для идентификаторов сохраняет значение в локальную переменную, для доступа к полям объекта - использует putField.
     * @param expression Выражение присваивания
     * @return Пустой Optional
     */
    override fun visitAssignExpr(expression: AssignExpression): Optional<ClassWriter> {
        val funcType = functionStack.peek()
        val ga = funcType.ga

        when (expression.left) {
            is IdentifierExpression -> {
                expression.right.accept(this)
                coerce(expression.right.realType, expression.left.realType)
                val index = funcType.localMap[expression.left.identifier.source]
                ga!!.storeLocal(index!!)
            }

            is PropertyAccessExpression -> {
                val lType = expression.left.realType
                val rType = expression.right.realType

                val root: Expression = expression.left.expression
                root.accept(this)

                val typeChain: MutableList<IxType?> = expression.left.typeChain
                val identifiers: MutableList<IdentifierExpression> = expression.left.identifiers
                for (i in 0..<typeChain.size - 2) {
                    val current = typeChain[i]
                    val next = typeChain[i + 1]
                    val fieldName = identifiers[i].identifier.source
                    ga!!.getField(Type.getType(current!!.descriptor), fieldName, Type.getType(next!!.descriptor))
                }

                expression.right.accept(this)

                coerce(rType, lType)

                ga!!.putField(
                    Type.getType(typeChain[typeChain.size - 2]!!.descriptor),
                    identifiers[identifiers.size - 1].identifier.source,
                    Type.getType(typeChain[typeChain.size - 1]!!.descriptor)
                )
            }

            else -> {
                ImplementationException().send(
                    api,
                    file,
                    expression,
                    "Assignment not implemented for any recipient but identifier yet"
                )
            }
        }
        return Optional.empty()
    }

    /**
     * Обрабатывает некорректное выражение. Возвращает пустой результат, так как такое выражение не должно встречаться на этапе генерации кода.
     * @param expression Некорректное выражение
     * @return Пустой Optional
     */
    override fun visitBad(expression: BadExpression): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для бинарных операций, включая арифметические, сравнения, логические операции и конкатенацию строк.
     * Для строк используется StringBuilder, для логических операций - short-circuit evaluation.
     * @param expression Бинарное выражение
     * @return Пустой Optional
     */
    override fun visitBinaryExpr(expression: BinaryExpression): Optional<ClassWriter> {
        val funcType = functionStack.peek()
        val ga = funcType.ga
        val left = expression.left
        val right = expression.right
        if (expression.realType == BuiltInType.STRING) {
            ga!!.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
            ga.visitInsn(Opcodes.DUP)
            ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", Init, "()V", false)

            expression.left.accept(this)

            val leftExprDescriptor = expression.left.realType.descriptor
            var descriptor = "($leftExprDescriptor)Ljava/lang/StringBuilder;"
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false)

            expression.right.accept(this)

            val rightExprDescriptor: String = expression.right.realType.descriptor!!
            descriptor = "($rightExprDescriptor)Ljava/lang/StringBuilder;"
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false)
            ga.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false
            )
        } else {
            when (expression.operator.type) {
                TokenType.AND -> {
                    val falseLabel = Label()
                    val successLabel = Label()

                    left.accept(this)
                    ga!!.ifZCmp(GeneratorAdapter.EQ, falseLabel)

                    right.accept(this)
                    ga.ifZCmp(GeneratorAdapter.EQ, falseLabel)
                    ga.push(true)
                    ga.goTo(successLabel)

                    ga.mark(falseLabel)
                    ga.push(false)

                    ga.mark(successLabel)
                }

                TokenType.OR -> {
                    val falseLabel = Label()
                    val successLabel = Label()
                    val endLabel = Label()

                    left.accept(this)
                    ga!!.ifZCmp(GeneratorAdapter.NE, successLabel)

                    right.accept(this)
                    ga.ifZCmp(GeneratorAdapter.NE, successLabel)
                    ga.goTo(falseLabel)

                    ga.mark(successLabel)
                    ga.push(true)
                    ga.goTo(endLabel)

                    ga.mark(falseLabel)
                    ga.push(false)

                    ga.mark(endLabel)
                }

                TokenType.XOR -> {
                    left.accept(this)
                    right.accept(this)
                    ga!!.visitInsn(Opcodes.IXOR)
                }

                TokenType.EQUAL, TokenType.NOTEQUAL, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE -> {
                    val cmpType: Type? = castAndAccept(ga!!, left, right, this)

                    val endLabel = Label()
                    val falseLabel = Label()

                    val opcode = when (expression.operator.type) {
                        TokenType.EQUAL -> GeneratorAdapter.NE
                        TokenType.NOTEQUAL -> GeneratorAdapter.EQ
                        TokenType.LT -> GeneratorAdapter.GE
                        TokenType.GT -> GeneratorAdapter.LE
                        TokenType.LE -> GeneratorAdapter.GT
                        TokenType.GE -> GeneratorAdapter.LT
                        else -> throw IllegalStateException("Unexpected value: " + expression.operator.type)
                    }
                    ga.ifCmp(cmpType, opcode, falseLabel)
                    ga.push(true)
                    ga.goTo(endLabel)

                    ga.mark(falseLabel)
                    ga.push(false)
                    ga.mark(endLabel)
                }

                TokenType.MOD -> {
                    val cmpType: Type? = castAndAccept(ga!!, left, right, this)

                    if (cmpType === Type.DOUBLE_TYPE) {
                        ga.visitInsn(Opcodes.DREM)
                    } else if (cmpType === Type.INT_TYPE) {
                        ga.visitInsn(Opcodes.IREM)
                    } else if (cmpType === Type.FLOAT_TYPE) {
                        ga.visitInsn(Opcodes.FREM)
                    }
                }

                TokenType.POW -> {
                    castAndAccept(ga!!, left, right, this)
                    ga.visitMethodInsn(
                        Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false
                    )
                    ga.visitInsn(Opcodes.D2I)
                }

                TokenType.ADD, TokenType.SUB, TokenType.MUL, TokenType.DIV -> arithmetic(
                    ga!!,
                    left,
                    right,
                    expression.operator,
                    expression.realType,
                    this
                )

                else -> {}
            }
        }
        return Optional.empty()
    }

    /**
     * Обрабатывает блок операторов, посещая каждый оператор в блоке последовательно.
     * @param statement Блок операторов
     * @return Пустой Optional
     */
    override fun visitBlockStmt(statement: BlockStatement): Optional<ClassWriter> {
        val previous = currentContext
        currentContext = statement.context
        statement.statements.filterNotNull().forEach { it.accept(this) }
        currentContext = previous
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для вызова функции или создания экземпляра структуры.
     * Обрабатывает как встроенные (glue) функции, так и пользовательские, включая специализацию дженериков.
     * @param expression Выражение вызова
     * @return Пустой Optional
     */
    override fun visitCall(expression: CallExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        val callee = expression.item.realType
        when (callee) {
            is ConstructorType -> emitAdt(expression.realType as AdtType, callee.variant, expression.arguments)
            is DefType -> {
                for ((index, parameter) in callee.parameters.withIndex()) {
                    val arg = expression.arguments[index]
                    val substitutions = callee.generics.mapIndexed { i, name -> GenericType(name, callee.declarationId, i) }
                        .associateWith { expression.specialization[it.key] ?: it }
                    val expected = TypeSubstitution.apply(parameter.second, substitutions)
                    arg.accept(this); coerce(arg.realType, expected)
                    if (parameter.second is GenericType) box(expected)
                }
                if (callee.hasGenerics() && (!callee.glue || callee.reifiedNative)) emitTags(callee.generics.map { expression.specialization.getValue(it) })
                val owner = if (callee.glue) callee.owner!! else callee.external?.fullRelativePath ?: source.fullRelativePath
                val name = if (callee.glue) (if (callee.isPrefixed) "_" else "") + callee.name else "_" + callee.name
                ga.invokeStatic(Type.getObjectType(owner), Method(name, functionDescriptor(callee)))
                if (callee.returnType is GenericType) fromObject(expression.realType)
                if (callee.returnType === BuiltInType.VOID || callee.returnType === UnitType) visitUnit(UnitExpression(expression.position!!))
                if (callee.returnType === NeverType) { ga.visitInsn(Opcodes.ACONST_NULL); ga.throwException() }
            }
            is LambdaType -> {
                expression.item.accept(this)
                if (callee.parameters.size > 4) { ga.push(callee.parameters.size); ga.newArray(ObjectType) }
                for ((index, parameter) in callee.parameters.withIndex()) {
                    if (callee.parameters.size > 4) { ga.dup(); ga.push(index) }
                    val arg = expression.arguments[index]; arg.accept(this); coerce(arg.realType, parameter.second); box(parameter.second)
                    if (callee.parameters.size > 4) ga.arrayStore(ObjectType)
                }
                ga.invokeInterface(Type.getType(callee.descriptor), Method("apply", callee.erasedApplyDescriptor()))
                if (callee.returnType === BuiltInType.VOID || callee.returnType === UnitType) { ga.pop(); visitUnit(UnitExpression(expression.position!!)) }
                else fromObject(expression.realType)
            }
            is StructType -> {
                val type = expression.realType as StructType
                val raw = if (type is AppliedStructType) type.base else type
                ga.newInstance(Type.getType(raw.descriptor)); ga.dup()
                for ((index, parameter) in raw.parameters.withIndex()) {
                    val arg = expression.arguments[index]
                    val expected = type.parameters[index].second
                    arg.accept(this); coerce(arg.realType, expected)
                    if (parameter.second is GenericType) box(expected)
                }
                if (raw.hasGenerics()) emitTag(TypeTags.expression(type))
                val descriptor = raw.parameters.joinToString("") { it.second.descriptor!! } + if (raw.hasGenerics()) "Ljava/lang/String;" else ""
                ga.invokeConstructor(Type.getType(raw.descriptor), Method("<init>", "($descriptor)V"))
            }
            else -> error("Unexpected checked callee ${callee.name}")
        }
        return Optional.empty()
    }

    /**
     * Обрабатывает пустое выражение. Не генерирует байт-код.
     * @param expression Пустое выражение
     * @return Пустой Optional
     */
    override fun visitEmpty(expression: EmptyExpression): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует пустой неизменяемый список с тегом типа элементов.
     * @param expression Выражение пустого списка
     * @return Пустой Optional
     */
    override fun visitEmptyList(expression: EmptyListExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        ga.newInstance(ListWrapperType)
        ga.dup()
        emitTag(TypeTags.expression((expression.realType as ListType).contentType))
        ga.invokeConstructor(ListWrapperType, Method(Init, "(Ljava/lang/String;)V"))
        return Optional.empty()
    }

    /**
     * Обрабатывает объявление перечисления. В данный момент не реализовано.
     * @param statement Оператор перечисления
     * @return Выбрасывает NotImplementedException
     */
    override fun visitEnum(statement: EnumStatement): Optional<ClassWriter> = Optional.empty()

    /**
     * Обрабатывает экспорт оператора, посещая внутренний оператор.
     * @param statement Оператор экспорта
     * @return Результат посещения внутреннего оператора
     */
    override fun visitExport(statement: ExportStatement): Optional<ClassWriter> {
        return statement.stmt.accept(this)
    }

    /**
     * Генерирует байт-код для выражения, используемого как оператор.
     * @param statement Оператор выражения
     * @return Пустой Optional
     */
    override fun visitExpressionStmt(statement: ExpressionStatement): Optional<ClassWriter> {
        statement.expression.accept(this)
        val type = statement.expression.realType
        if (statement.expression !is AssignExpression && type !is UnknownType && type != BuiltInType.VOID && type !== NeverType) {
            val ga = functionStack.peek().ga!!
            if (Type.getType(type.descriptor).size == 2) ga.pop2() else ga.pop()
        }
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для цикла for. Поддерживает итерацию по идентификатору или вызову, возвращающему итератор.
     * @param statement Оператор цикла for
     * @return Пустой Optional
     */
    override fun visitFor(statement: ForStatement): Optional<ClassWriter> {
        val funcType = functionStack.peek()
        val ga = functionStack.peek().ga
        currentContext = statement.block.context
        val startLabel = Label()
        val endLabel = Label()

        // Evaluate the iterable once and keep a distinct iterator for every loop.
        statement.expression.accept(this)
        if (statement.expression.realType === BuiltInType.STRING) {
            ga!!.invokeStatic(Type.getType(DataIterables::class.java), Method("characters", "(Ljava/lang/String;)Ljava/lang/Iterable;"))
            ga.invokeInterface(Type.getType(Iterable::class.java), Method("iterator", "()Ljava/util/Iterator;"))
        }
        if (statement.expression.realType is ListType) {
            ga!!.checkCast(ListWrapperType)
            ga.invokeVirtual(ListWrapperType, Method("iterator", "()Ljava/util/Iterator;"))
        }
        val iteratorIndex = ga!!.newLocal(IteratorType)
        ga.storeLocal(iteratorIndex, IteratorType)
        ga.mark(startLabel)
        ga.loadLocal(iteratorIndex, IteratorType)
        ga.invokeInterface(IteratorType, Method("hasNext", "()Z"))
        ga.visitJumpInsn(Opcodes.IFEQ, endLabel)
        ga.loadLocal(iteratorIndex, IteratorType)
        ga.invokeInterface(IteratorType, Method("next", "()Ljava/lang/Object;"))

        val elementType = when (val exprType = statement.expression.realType) {
            BuiltInType.STRING -> BuiltInType.CHAR
            is ListType -> exprType.contentType
            else -> BuiltInType.INT
        }
        if (elementType is BuiltInType) {
            elementType.doUnboxing(ga)
        } else {
            ga.checkCast(Type.getType(elementType.descriptor))
        }
        statement.localExprIndex = ga.newLocal(Type.getType(elementType.descriptor))
        funcType.localMap[statement.name.source] = statement.localExprIndex
        ga.storeLocal(statement.localExprIndex, Type.getType(elementType.descriptor))

        statement.block.accept(this)

        ga.goTo(startLabel)
        ga.mark(endLabel)
        currentContext = currentContext!!.parent
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для объявления функции, включая специализации для дженериков.
     * Создает метод в текущем ClassWriter с соответствующим дескриптором и генерирует тело функции.
     * @param statement Оператор объявления функции
     * @return Пустой Optional
     */
    override fun visitFunctionStmt(statement: DefStatement): Optional<ClassWriter> {
        val function = currentContext!!.getVariableTyped<DefType>(statement.name.source)!!
        functionStack.push(function)
        val descriptor = if (function.name == "main") "([Ljava/lang/String;)V" else functionDescriptor(function)
        val name = if (function.name == "main") "main" else "_" + function.name
        val access = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
        val method = cw.visitMethod(access, name, descriptor, null, null)
        function.ga = GeneratorAdapter(method, access, name, descriptor)
        function.localMap.clear(); function.argMap.clear()
        function.parameters.forEachIndexed { index, p -> function.argMap[p.first] = index }
        val previous = currentContext
        currentContext = statement.body!!.context
        statement.body.accept(this)
        function.ga!!.endMethod()
        currentContext = previous
        functionStack.pop()
        return Optional.empty()
    }

    /**
     * Обрабатывает группирующее выражение, посещая внутреннее выражение.
     * @param expression Группирующее выражение
     * @return Пустой Optional
     */
    override fun visitGroupingExpr(expression: GroupingExpression): Optional<ClassWriter> {
        expression.expression.accept(this)
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для загрузки значения идентификатора из локальной переменной или аргумента метода.
     * @param expression Выражение идентификатора
     * @return Пустой Optional
     */
    override fun visitIdentifierExpr(expression: IdentifierExpression): Optional<ClassWriter> {
        val function = functionStack.peek()
        val ga = function.ga!!
        val name = expression.identifier.source
        val declaration = currentContext!!.getVariable(name)
        if (declaration is ConstructorType && declaration.variant.fields.isEmpty()) {
            emitAdt(expression.realType as AdtType, declaration.variant, emptyList())
            return Optional.empty()
        }
        val local = function.localMap[name]
        if (local != null) ga.loadLocal(local)
        else ga.loadArg(function.argMap[name] ?: error("Missing checked local $name"))
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для условного оператора if, включая ветку else если она присутствует.
     * @param statement Оператор if
     * @return Пустой Optional
     */
    override fun visitIf(statement: IfStatement): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        val end = Label()
        val otherwise = Label()
        val previous = currentContext
        statement.condition.accept(this)
        ga.ifZCmp(GeneratorAdapter.EQ, otherwise)
        statement.trueBlock.accept(this)
        ga.goTo(end); ga.mark(otherwise)
        statement.falseStatement?.accept(this)
        ga.mark(end)
        currentContext = previous
        return Optional.empty()
    }

    /**
     * Обрабатывает оператор использования (use). Не требует генерации байт-кода.
     * @param statement Оператор use
     * @return Пустой Optional
     */
    override fun visitUse(statement: UseStatement): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует Option-доступ к списку или прежний доступ к односимвольной строке.
     * @param expression Выражение доступа по индексу
     * @return Пустой Optional
     */
    override fun visitIndexAccess(expression: IndexAccessExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        expression.left.accept(this)
        if (expression.left.realType == BuiltInType.STRING) {
            expression.right.accept(this)
            ga.dup()
            ga.push(1)
            ga.math(GeneratorAdapter.ADD, Type.INT_TYPE)
            ga.invokeVirtual(Type.getType(String::class.java), Method("substring", "(II)Ljava/lang/String;"))
        } else {
            expression.right.accept(this)
            ga.invokeStatic(Type.getType(com.kingmang.ixion.modules.Prelude::class.java),
                Method("get", "(" + ListWrapperType!!.descriptor + "I)Lcom/kingmang/ixion/runtime/AdtValue;"))
        }
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для литералов встроенных типов (int, float, double, boolean, string).
     * @param expression Выражение литерала
     * @return Пустой Optional
     */
    override fun visitLiteralExpr(expression: LiteralExpression): Optional<ClassWriter> {
        if (expression.realType is BuiltInType) {
            val transformed =
                TypeResolver.getValueFromString(expression.literal.source, getFromToken(expression.literal.type)!!)
            val ga = functionStack.peek().ga
            when (expression.realType) {
                BuiltInType.CHAR -> ga!!.push((transformed as Char).code)
                BuiltInType.INT -> ga!!.push(transformed as Int)
                BuiltInType.FLOAT -> ga!!.push(transformed as Float)
                BuiltInType.DOUBLE -> ga!!.push(transformed as Double)
                BuiltInType.BOOLEAN -> ga!!.push(transformed as Boolean)
                BuiltInType.STRING -> ga!!.push(transformed as String?)
            }
        } else {
            ImplementationException().send(
                api,
                source.file,
                expression,
                "This should never happen. All literals should be builtin, for now."
            )
        }
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для создания списка литералов с помощью ListWrapper.
     * Каждый элемент добавляется через метод add ArrayList.
     * @param expression Выражение списка литералов
     * @return Пустой Optional
     */
    override fun visitLiteralList(expression: LiteralListExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        ga.newInstance(ListWrapperType)
        ga.dup()
        ga.newInstance(ArrayListType)
        ga.dup()
        ga.invokeConstructor(ArrayListType, Method(Init, "()V"))
        val content = (expression.realType as ListType).contentType
        for (entry in expression.entries) {
            ga.dup()
            entry.accept(this)
            coerce(entry.realType, content)
            box(content)
            ga.invokeVirtual(ArrayListType, Method("add", "(Ljava/lang/Object;)Z"))
            ga.pop()
        }
        emitTag(TypeTags.expression(content))
        ga.invokeConstructor(ListWrapperType, Method(Init, "(Ljava/util/Collection;Ljava/lang/String;)V"))
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для оператора match, преобразуя его в последовательность проверок instanceof и условных переходов.
     * Поддерживает сопоставление с типами ListType, BuiltInType и StructType.
     * @param statement Оператор match
     * @return Пустой Optional
     */


    /**
     * Обрабатывает доступ к модулю. Не требует генерации байт-кода.
     * @param expression Выражение доступа к модулю
     * @return Пустой Optional
     */
    override fun visitModuleAccess(expression: ModuleAccessExpression): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Обрабатывает оператор параметра. В данный момент не реализовано.
     * @param statement Оператор параметра
     * @return Выбрасывает NotImplementedException
     */
    override fun visitParameterStmt(statement: ParameterStatement): Optional<ClassWriter> {
        throw NotImplementedException("method not implemented")
    }

    /**
     * Генерирует байт-код для постфиксных операций (инкремент/декремент) над переменными встроенных типов.
     * @param expression Постфиксное выражение
     * @return Пустой Optional
     */
    override fun visitPostfixExpr(expression: PostfixExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        expression.expression.accept(this)
        if (expression.realType is BuiltInType) {
            if (Type.getType(expression.realType.descriptor).size == 2) ga.dup2() else ga.dup()
            (expression.realType as BuiltInType).pushOne(ga)
            val op: Int = when (expression.operator.type) {
                TokenType.PLUSPLUS -> (expression.realType as BuiltInType).addOpcode
                TokenType.MINUSMINUS -> (expression.realType as BuiltInType).subtractOpcode
                else -> throw IllegalStateException("Unexpected value: " + expression.operator.type)
            }

            ga.visitInsn(op)
            if (expression.expression is IdentifierExpression) {
                val function = functionStack.peek()
                val name = expression.expression.identifier.source
                val local = function.localMap[name]
                if (local != null) ga.storeLocal(local) else ga.storeArg(function.argMap[name]!!)
            }
        } else {
            exit("postfix only works with builtin types", 49)
        }
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для префиксных операций. В настоящее время поддерживает только унарный минус для числовых типов.
     * @param expression Префиксное выражение
     * @return Пустой Optional
     */
    override fun visitPrefix(expression: PrefixExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga

        expression.right.accept(this)

        val t = expression.right.realType
        if (expression.operator.type == TokenType.SUB && t is BuiltInType) {
            ga!!.visitInsn(t.negOpcode)
            expression.realType = t
        }

        return Optional.empty()
    }

    /**
     * Генерирует байт-код для доступа к полям объекта, включая поддержку монотипизированных структур.
     * @param expression Выражение доступа к свойству
     * @return Пустой Optional
     */
    override fun visitPropertyAccess(expression: PropertyAccessExpression): Optional<ClassWriter> {
        val ga = functionStack.peek().ga!!
        expression.expression.accept(this)
        for (index in expression.identifiers.indices) {
            val type = expression.typeChain[index] as StructType
            val raw = if (type is AppliedStructType) type.base else type
            val name = expression.identifiers[index].identifier.source
            val field = raw.parameters.first { it.first == name }.second
            ga.push(raw.parameters.indexOfFirst { it.first == name })
            ga.invokeInterface(Type.getType(IxDataValue::class.java), Method("ixField", "(I)Ljava/lang/Object;"))
            fromObject(expression.typeChain[index + 1]!!)
        }
        return Optional.empty()
    }

    /**
     * Обрабатывает лямбда-выражение. В данный момент не реализовано.
     * @param expression Лямбда-выражение
     * @return Пустой Optional
     */
    override fun visitLambda(expression: LambdaExpression): Optional<ClassWriter> {
        val lambdaType = expression.realType as LambdaType
        val outer = functionStack.peek()
        val owner = source.fullRelativePath
        val methodName = "lambda$" + lambdaCounter++
        val captures = expression.captures
        val generic = outer.generics.toMutableList()
        val hidden = if (generic.isEmpty()) "" else "[Ljava/lang/String;"
        val variadic = lambdaType.parameters.size > 4
        val unit = lambdaType.returnType === BuiltInType.VOID || lambdaType.returnType === UnitType
        val physicalResult = if (unit || variadic) BuiltInType.ANY else lambdaType.returnType
        val captureDescriptor = captures.joinToString("") { it.second.descriptor!! } + hidden
        val formalDescriptor = if (variadic) "[Ljava/lang/Object;" else lambdaType.parameters.joinToString("") { it.second.descriptor!! }
        val descriptor = "($captureDescriptor$formalDescriptor)${physicalResult.descriptor}"
        val access = Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC
        val method = cw.visitMethod(access, methodName, descriptor, null, null)
        val physicalParameters = (captures + (if (generic.isEmpty()) emptyList() else listOf("__ixTypes" to ExternalType(Array<String>::class.java))) +
            if (variadic) emptyList() else lambdaType.parameters).toMutableList()
        val implementation = DefType(methodName, physicalParameters, generic)
        implementation.returnType = physicalResult
        implementation.bridgeVoidToObject = unit
        if (generic.isNotEmpty()) implementation.reifiedArgumentIndex = captures.size
        implementation.ga = GeneratorAdapter(method, access, methodName, descriptor)
        physicalParameters.forEachIndexed { index, parameter -> implementation.argMap[parameter.first] = index }
        functionStack.push(implementation)
        val previous = currentContext
        currentContext = expression.body.context
        if (variadic) {
            val arrayIndex = captures.size + if (generic.isEmpty()) 0 else 1
            for ((index, parameter) in lambdaType.parameters.withIndex()) {
                implementation.ga!!.loadArg(arrayIndex); implementation.ga!!.push(index); implementation.ga!!.arrayLoad(ObjectType)
                fromObject(parameter.second)
                val local = implementation.ga!!.newLocal(Type.getType(parameter.second.descriptor))
                implementation.ga!!.storeLocal(local); implementation.localMap[parameter.first] = local
            }
        }
        expression.body.accept(this)
        implementation.ga!!.endMethod()
        currentContext = previous
        functionStack.pop()
        for ((name, type) in captures) {
            val reference = IdentifierExpression(expression.position, com.kingmang.ixion.lexer.Token(TokenType.IDENTIFIER, expression.position!!.line, expression.position.col, name))
            reference.realType = type; reference.accept(this)
        }
        if (generic.isNotEmpty()) outer.ga!!.loadArg(tagArgument(outer))
        val bootstrap = Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false)
        val handle = Handle(Opcodes.H_INVOKESTATIC, owner, methodName, descriptor, false)
        outer.ga!!.invokeDynamic("apply", "($captureDescriptor)${lambdaType.descriptor}", bootstrap,
            Type.getMethodType(lambdaType.erasedApplyDescriptor()), handle,
            Type.getMethodType(if (variadic) lambdaType.erasedApplyDescriptor() else lambdaInstantiationDescriptor(lambdaType)))
        emitTag(TypeTags.expression(lambdaType))
        outer.ga!!.invokeStatic(Type.getType(FunctionTags::class.java), Method("register", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"))
        outer.ga!!.checkCast(Type.getType(lambdaType.descriptor))
        return Optional.empty()
    }

    /**
     * Обрабатывает доступ к перечислению. В данный момент не реализовано.
     * @param expression Выражение доступа к перечислению
     * @return Пустой Optional
     */
    override fun visitEnumAccess(expression: EnumAccessExpression): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для оператора return, включая упаковку примитивных значений для функций с UnionType.
     * @param statement Оператор return
     * @return Пустой Optional
     */
    override fun visitReturnStmt(statement: ReturnStatement): Optional<ClassWriter> {
        val function = functionStack.peek()
        val ga = function.ga!!
        if (statement.expression !is EmptyExpression) {
            statement.expression!!.accept(this)
            coerce(statement.expression.realType, function.returnType)
            if (function.returnType === UnitType) ga.pop()
            ga.visitInsn(JvmTypeLayout.result(SemanticTypes.of(function.returnType)).returnOpcode)
        } else if (function.bridgeVoidToObject) {
            ga.visitInsn(Opcodes.ACONST_NULL); ga.visitInsn(Opcodes.ARETURN)
        } else ga.visitInsn(Opcodes.RETURN)
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для объявления структуры: создает внутренний класс с полями, конструктором и методом toString.
     * @param statement Оператор структуры
     * @return Optional с ClassWriter внутреннего класса
     */
    override fun visitStruct(statement: StructStatement): Optional<ClassWriter> {
        val innerCw = ClassWriter(FLAGS)
        val structType = currentContext!!.getVariableTyped<StructType>(statement.name.source)

        val name = structType!!.name
        val innerName = source.fullRelativePath + "$" + name

        innerCw.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER, innerName, null, "java/lang/Object", arrayOf("com/kingmang/ixion/runtime/IxDataValue"))
        val innerFlags = PublicStatic or Opcodes.ACC_FINAL
        cw.visitInnerClass(innerName, source.fullRelativePath, name, innerFlags)
        innerCw.visitInnerClass(innerName, source.fullRelativePath, name, innerFlags)

        val constructorDescriptor = StringBuilder()

        for (pair in structType.parameters) {
            val type = pair.second
            val descriptor: String = type.descriptor!!
            val n = pair.first

            val fieldVisitor = innerCw.visitField(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, n, descriptor, null, null)
            fieldVisitor.visitEnd()

            constructorDescriptor.append(descriptor)
        }

        if (structType.hasGenerics()) innerCw.visitField(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, "__ixType", "Ljava/lang/String;", null, null).visitEnd()
        val tagDescriptor = if (structType.hasGenerics()) "Ljava/lang/String;" else ""
        var descriptor = "($constructorDescriptor$tagDescriptor)V"
        val mv = innerCw.visitMethod(Opcodes.ACC_PUBLIC, Init, descriptor, null, null)
        val ga = GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, Init, descriptor)

        val ownerInternalName = source.fullRelativePath + "$" + name

        ga.loadThis()
        ga.invokeConstructor(ObjectType, Method(Init, "()V"))

        for (i in structType.parameters.indices) {
            val type = structType.parameters[i].second
            descriptor = type.descriptor!!
            val n = structType.parameters[i].first
            ga.visitVarInsn(Opcodes.ALOAD, 0)
            ga.loadArg(i)

            ga.visitFieldInsn(Opcodes.PUTFIELD, ownerInternalName, n, descriptor)
        }

        if (structType.hasGenerics()) {
            ga.loadThis(); ga.loadArg(structType.parameters.size)
            ga.putField(Type.getObjectType(innerName), "__ixType", Type.getType(String::class.java))
        }
        ga.returnValue()
        ga.endMethod()

        val typeMethod = innerCw.visitMethod(Opcodes.ACC_PUBLIC, "ixType", "()Ljava/lang/String;", null, null)
        val typeGa = GeneratorAdapter(typeMethod, Opcodes.ACC_PUBLIC, "ixType", "()Ljava/lang/String;")
        if (structType.hasGenerics()) { typeGa.loadThis(); typeGa.getField(Type.getObjectType(innerName), "__ixType", Type.getType(String::class.java)) }
        else typeGa.push(TypeTags.constant(structType))
        typeGa.returnValue(); typeGa.endMethod()
        val fieldMethod = innerCw.visitMethod(Opcodes.ACC_PUBLIC, "ixField", "(I)Ljava/lang/Object;", null, null)
        val fieldGa = GeneratorAdapter(fieldMethod, Opcodes.ACC_PUBLIC, "ixField", "(I)Ljava/lang/Object;")
        for ((index, field) in structType.parameters.withIndex()) {
            val next = Label()
            fieldGa.loadArg(0); fieldGa.push(index); fieldGa.ifICmp(GeneratorAdapter.NE, next)
            fieldGa.loadThis(); fieldGa.getField(Type.getObjectType(innerName), field.first, Type.getType(field.second.descriptor))
            if (field.second is BuiltInType) (field.second as BuiltInType).doBoxing(fieldGa)
            fieldGa.returnValue(); fieldGa.mark(next)
        }
        fieldGa.throwException(Type.getType(IndexOutOfBoundsException::class.java), "Invalid product field")
        fieldGa.endMethod()

        BytecodeGenerator.addToString(innerCw, structType, constructorDescriptor.toString(), ownerInternalName)

        this.structWriters[structType] = innerCw

        return Optional.of(innerCw)
    }

    /**
     * Обрабатывает оператор типа. В данный момент не реализовано.
     * @param statement Оператор типа
     * @return Выбрасывает NotImplementedException
     */
    override fun visitTypeAlias(statement: TypeStatement): Optional<ClassWriter> {
        throw NotImplementedException("method not implemented")
    }

    /**
     * Обрабатывает объявление union-типа. Не требует генерации байт-кода.
     * @param statement Оператор union-типа
     * @return Пустой Optional
     */
    override fun visitUnionType(statement: UnionTypeStatement): Optional<ClassWriter> {
        return Optional.empty()
    }

    /**
     * Генерирует байт-код для объявления переменной: вычисляет значение выражения и сохраняет в локальную переменную.
     * @param statement Оператор переменной
     * @return Пустой Optional
     */
    override fun visitVariable(statement: VariableStatement): Optional<ClassWriter> {
        val funcType = functionStack.peek()

        var type = currentContext!!.getVariable(statement.identifier())
        statement.expression.accept(this)
        coerce(statement.expression.realType, type!!)

        statement.localIndex = funcType.ga!!.newLocal(Type.getType(type!!.descriptor))
        funcType.localMap[statement.identifier()] = statement.localIndex
        funcType.ga!!.storeLocal(statement.localIndex, Type.getType(type.descriptor))

        if (statement.expression is PostfixExpression) {
            statement.expression.localIndex = statement.localIndex
        }

        return Optional.empty()
    }

    /**
     * Генерирует байт-код для цикла while с проверкой условия в начале каждой итерации.
     * @param statement Оператор while
     * @return Пустой Optional
     */
    override fun visitWhile(statement: WhileStatement): Optional<ClassWriter> {
        val funcType = functionStack.peek()
        val ga = funcType.ga
        val endLabel = Label()
        val startLabel = Label()

        ga!!.mark(startLabel)
        statement.condition.accept(this)

        currentContext = statement.block.context
        ga.ifZCmp(GeneratorAdapter.EQ, endLabel)
        statement.block.accept(this)
        ga.goTo(startLabel)
        ga.mark(endLabel)
        currentContext = currentContext!!.parent
        return Optional.empty()
    }

    companion object {
        const val FLAGS: Int = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS
        const val CLASS_VERSION: Int = 61

        /**
         * Приводит два выражения к общему типу и генерирует код для их вычисления.
         * Используется для операций сравнения и арифметики.
         * @param ga GeneratorAdapter для генерации байт-кода
         * @param left Левое выражение
         * @param right Правое выражение
         * @param visitor Посетитель для рекурсивного посещения выражений
         * @return Тип, к которому были приведены выражения
         */
        private fun castAndAccept(
            ga: GeneratorAdapter,
            left: Expression,
            right: Expression,
            visitor: CodegenVisitor
        ): Type? {
            val lWide: Int = widenings.getOrDefault(left.realType as BuiltInType, -1)!!
            val rWide: Int = widenings.getOrDefault(right.realType as BuiltInType, -1)!!
            val lType = Type.getType(left.realType.descriptor)
            val rType = Type.getType(right.realType.descriptor)

            var cmpType = lType

            if (lWide != -1 && rWide != -1) {
                if (lWide > rWide) {
                    left.accept(visitor)
                    right.accept(visitor)
                    ga.cast(rType, lType)
                } else if (lWide < rWide) {
                    left.accept(visitor)
                    ga.cast(lType, rType)
                    right.accept(visitor)
                    cmpType = rType
                } else {
                    left.accept(visitor)
                    right.accept(visitor)
                }
            } else {
                left.accept(visitor)
                right.accept(visitor)
            }
            return cmpType
        }

        /**
         * Генерирует байт-код для арифметических операций, включая приведение типов при необходимости.
         * @param ga GeneratorAdapter для генерации байт-кода
         * @param left Левое выражение
         * @param right Правое выражение
         * @param operator Токен оператора
         * @param goalType Ожидаемый тип результата
         * @param visitor Посетитель для рекурсивного посещения выражений
         */
        fun arithmetic(
            ga: GeneratorAdapter,
            left: Expression,
            right: Expression,
            operator: Token,
            goalType: IxType?,
            visitor: CodegenVisitor
        ) {
            var goalType = goalType
            if (left.realType == right.realType) {
                left.accept(visitor)
                right.accept(visitor)
            } else {
                if (left.realType === BuiltInType.INT && right.realType === BuiltInType.FLOAT) {
                    left.accept(visitor)
                    ga.visitInsn(Opcodes.I2F)
                    right.accept(visitor)
                    goalType = BuiltInType.FLOAT
                } else if (left.realType === BuiltInType.FLOAT && right.realType === BuiltInType.INT) {
                    left.accept(visitor)
                    right.accept(visitor)
                    ga.visitInsn(Opcodes.I2F)
                    goalType = BuiltInType.FLOAT
                } else if (left.realType === BuiltInType.INT && right.realType === BuiltInType.DOUBLE) {
                    left.accept(visitor)
                    ga.visitInsn(Opcodes.I2D)
                    right.accept(visitor)
                    goalType = BuiltInType.DOUBLE
                } else if (left.realType === BuiltInType.DOUBLE && right.realType === BuiltInType.INT) {
                    left.accept(visitor)
                    right.accept(visitor)
                    ga.visitInsn(Opcodes.I2D)
                    goalType = BuiltInType.DOUBLE
                }
            }
            if (goalType is BuiltInType) {
                val op = when (operator.type) {
                    TokenType.ADD -> goalType.addOpcode
                    TokenType.SUB -> goalType.subtractOpcode
                    TokenType.MUL -> goalType.multiplyOpcode
                    TokenType.DIV -> goalType.divideOpcode
                    TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE -> 0
                    else -> throw IllegalStateException("Unexpected value: " + operator.type)
                }
                ga.visitInsn(op)
            } else {
                exit("need a test case here", 452)
            }
        }

        private fun lambdaInstantiationDescriptor(lambdaType: LambdaType): String {
            val params = lambdaType.parameters.joinToString("") { boxedDescriptor(it.second) }
            val returnDescriptor = if (lambdaType.returnType === UnitType) "Ljava/lang/Object;" else boxedDescriptor(lambdaType.returnType)
            return "($params)$returnDescriptor"
        }

        private fun boxedDescriptor(type: IxType): String {
            return when (type) {
                BuiltInType.INT -> "Ljava/lang/Integer;"
                BuiltInType.FLOAT -> "Ljava/lang/Float;"
                BuiltInType.DOUBLE -> "Ljava/lang/Double;"
                BuiltInType.BOOLEAN -> "Ljava/lang/Boolean;"
                BuiltInType.CHAR -> "Ljava/lang/Character;"
                BuiltInType.VOID -> "Ljava/lang/Object;"
                else -> type.descriptor!!
            }
        }
    }
}
