package com.kingmang.ixion.semantic

import com.kingmang.ixion.api.IxApi
import com.kingmang.ixion.api.IxFile
import com.kingmang.ixion.ast.*
import com.kingmang.ixion.exception.IdentifierNotFoundException
import com.kingmang.ixion.exception.RedeclarationException
import com.kingmang.ixion.exception.TypeDeclarationException
import com.kingmang.ixion.modules.Modules
import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.types.DeclarationId
import com.kingmang.ixion.types.DeclarationKind
import com.kingmang.ixion.typechecker.TypeUtils
import com.kingmang.ixion.typechecker.TypeSyntax
import java.util.IdentityHashMap

/** Register all nominal declarations before resolving fields, aliases and function signatures. */
class DeclarationCollector(private val api: IxApi, private val files: Collection<IxFile>) {
    private val declarations = IdentityHashMap<IxFile, Map<String, Statement>>()
    private val aliases = IdentityHashMap<TypeAliasStatement, IxType>()
    private val visitingAliases = ArrayList<TypeAliasStatement>()

    fun collect() {
        files.forEach(::installPrelude)
        files.forEach(::register)
        files.forEach { file ->
            for (declaration in declarations.getValue(file).values) {
                when (declaration) {
                    is TypeAliasStatement -> resolveAlias(file, declaration)
                    is StructStatement -> resolveStruct(file, declaration)
                    is DefStatement -> resolveFunction(file, declaration)
                    is EnumStatement -> resolveEnum(file, declaration)
                }
            }
        }
        for (file in files) {
            for ((name, functions) in file.rootContext.localFunctions()) {
                val seen = hashSetOf<List<com.kingmang.ixion.types.Type>>()
                for (function in functions) {
                    val signature = function.parameters.map { com.kingmang.ixion.typechecker.SemanticTypes.of(it.second) }
                    if (!seen.add(signature)) {
                        TypeDeclarationException().send(api, file.file, declarations.getValue(file)
                            .values.filterIsInstance<DefStatement>().first { it.functionType === function },
                            "duplicate function signature $name(${function.parameters.joinToString { it.second.name?.lowercase() ?: "?" }})")
                    }
                }
            }
        }
        files.forEach { file ->
            for ((_, node) in declarations.getValue(file)) if (node is EnumStatement) {
                val type = file.rootContext.getType(node.name.source) as AdtType
                registerConstructors(file, node.name.source, type.declaration)
            }
        }
        bindExports()
        validateRecursion()
        validatePublicSignatures()
    }

    fun bindExports() {
        files.forEach(::publishExports)
        files.forEach { file ->
            for (imported in file.imports.values.filterNotNull()) {
                for ((name, type) in imported.exportedTypes) {
                    file.rootContext.addType("${imported.name}::$name", type)
                }
                for ((name, type) in imported.exports) {
                    if (type != null && name != null) {
                        if (type is DefType) type.external = imported
                        file.rootContext.addVariable("${imported.name}::$name", type)
                    }
                }
                for ((name, functions) in imported.exportedFunctions) {
                    functions.forEach { function ->
                        function.external = imported
                        file.rootContext.addFunction("${imported.name}::$name", function)
                    }
                }
            }
        }
    }

    fun validatePublicSignatures() {
        val owners = files.associateBy { it.fullRelativePath }
        for (file in files) {
            for (outer in file.statements.filterIsInstance<ExportStatement>()) {
                val name = exportedName(outer.stmt) ?: continue
                val type = file.exportedTypes[name] ?: file.exports[name]
                val seen = hashSetOf<com.kingmang.ixion.types.Type>()
                val nominal = hashSetOf<DeclarationId>()
                fun check(type: IxType) {
                    if (type !is ConstructorType && !seen.add(com.kingmang.ixion.typechecker.SemanticTypes.of(type))) return
                    when (type) {
                        is DefType -> {
                            type.parameters.forEach { check(it.second) }
                            check(type.returnType)
                        }

                        is StructType -> {
                            val owner = owners[type.declarationId.module]
                            if (owner != null && owner.exportedTypes[type.declarationId.name] != type) {
                                TypeDeclarationException().send(
                                    api, file.file, outer,
                                    "public signature $name exposes private type ${type.declarationId.module}::${type.name}"
                                )
                            }
                            if (type is AppliedStructType) type.arguments.forEach(::check)
                            if (!nominal.add(type.declarationId)) return
                            type.parameters.forEach { check(it.second) }
                        }

                        is AdtType -> {
                            val owner = owners[type.declaration.id.module]
                            if (owner != null && owner.exportedTypes[type.declaration.id.name] !is AdtType)
                                TypeDeclarationException().send(
                                    api,
                                    file.file,
                                    outer,
                                    "public signature $name exposes private type ${type.name}"
                                )
                            type.arguments.forEach(::check)
                            if (!nominal.add(type.declaration.id)) return
                            type.declaration.variants.forEach { v -> type.fields(v).forEach { check(it.second) } }
                        }

                        is AliasType -> check(type.body)
                        is TupleType -> type.elements.forEach(::check)
                        is ListType -> check(type.contentType)
                        is UnionType -> type.types.forEach(::check)
                        is LambdaType -> {
                            type.parameters.forEach { check(it.second) }
                            check(type.returnType)
                        }
                    }
                }
                type?.let(::check)
                file.exportedFunctions[name].orEmpty().forEach(::check)
            }
        }
    }

    private fun validateRecursion() {
        for (file in files) for (node in declarations.getValue(file).values) {
            val name = when (node) {
                is StructStatement -> node.name.source; is EnumStatement -> node.name.source; else -> continue
            }
            val root = file.rootContext.getType(name)!!
            val target = when (root) {
                is StructType -> root.declarationId; is AdtType -> root.declaration.id; else -> continue
            }
            val seen = hashSetOf<Pair<DeclarationId, Boolean>>()
            fun walk(type: IxType, positive: Boolean) {
                val id = when (type) {
                    is StructType -> type.declarationId; is AdtType -> type.declaration.id; else -> null
                }
                if (id == target) {
                    if (!positive) TypeDeclarationException().send(
                        api,
                        file.file,
                        node,
                        "negative recursive occurrence of $name"
                    )
                    return
                }
                if (id != null && !seen.add(id to positive)) return
                when (type) {
                    is StructType -> type.parameters.forEach { walk(it.second, positive) }
                    is AdtType -> type.declaration.variants.forEach { variant ->
                        type.fields(variant).forEach { walk(it.second, positive) }
                    }

                    is LambdaType -> {
                        type.parameters.forEach { walk(it.second, !positive) }; walk(type.returnType, positive)
                    }

                    is ListType -> walk(type.contentType, positive)
                    is TupleType -> type.elements.forEach { walk(it, positive) }
                    is UnionType -> type.types.forEach { walk(it, positive) }
                }
            }
            when (root) {
                is StructType -> root.parameters.forEach { walk(it.second, true) }
                is AdtType -> root.declaration.variants.forEach { variant ->
                    root.fields(variant).forEach { walk(it.second, true) }
                }
            }
        }
    }

    private fun installPrelude(file: IxFile) {
        fun install(name: String, parameters: List<String>, variants: (List<GenericType>) -> List<AdtVariant>) {
            val id = DeclarationId("<prelude>", name, DeclarationKind.ADT)
            val params = parameters.mapIndexed { i, p -> GenericType(p, id, i) }
            val declaration = AdtDeclaration(id, params)
            declaration.variants.addAll(variants(params))
            file.rootContext.addType(name, declaration.applied(params))
            registerConstructors(file, name, declaration)
        }

        val option = PreludeTypes.option
        file.rootContext.addType("Option", option.applied(option.parameters))
        registerConstructors(file, "Option", option)
        install("Result", listOf("T", "E")) { p ->
            listOf(
                AdtVariant("Ok", listOf("value" to p[0])),
                AdtVariant("Err", listOf("error" to p[1]))
            )
        }
    }

    private fun registerConstructors(file: IxFile, name: String, declaration: AdtDeclaration) {
        for (variant in declaration.variants) file.rootContext.addVariable(
            "$name::${variant.name}",
            ConstructorType(declaration, variant)
        )
    }

    private fun resolveEnum(file: IxFile, node: EnumStatement) {
        val declaration = (file.rootContext.getType(node.name.source) as AdtType).declaration
        val parameters = declaration.parameters.associateBy { it.key }
        checkFields(file, node, node.variants.map { it.name.source })
        for (variant in node.variants) {
            checkFields(file, node, variant.fields.map { it.name.source })
            declaration.variants.add(
                AdtVariant(
                    variant.name.source,
                    variant.fields.map { it.name.source to resolve(file, it.type!!, parameters) })
            )
        }
    }

    private fun register(file: IxFile) {
        val entries = linkedMapOf<String, Statement>()
        declarations[file] = entries
        val qualifiers = hashSetOf<String>()
        for (outer in file.statements) {
            val statement = if (outer is ExportStatement) outer.stmt else outer
            when (statement) {
                is UseStatement -> {
                    val module = statement.stringLiteral.source
                    val qualifier = module.substringAfterLast('/')
                    if (!qualifiers.add(qualifier)) {
                        TypeDeclarationException().send(
                            api,
                            file.file,
                            statement,
                            "ambiguous module qualifier $qualifier"
                        )
                    }
                    val localModule = file.imports.values.filterNotNull().any { it.name == qualifier }
                    if (!localModule && Modules.modules.containsKey(module)) {
                        for ((name, type) in Modules.getTypes(module)) {
                            file.rootContext.addType("$module::$name", type)
                            if (type is AdtType) registerConstructors(file, "$module::$name", type.declaration)
                        }
                        for (function in Modules.getExports(module).filterNotNull()) {
                            file.rootContext.addVariableOrError(
                                api,
                                "$module::${function.name}",
                                function,
                                file.file,
                                statement
                            )
                        }
                    }
                }

                is StructStatement -> {
                    if (!claim(file, entries, statement.name.source, statement)) continue
                    val generics = statement.generics.orEmpty().map { it.source }.toMutableList()
                    checkParameters(file, statement, generics)
                    val type = StructType(
                        statement.name.source, arrayListOf(), generics,
                        DeclarationId(file.fullRelativePath, statement.name.source, DeclarationKind.STRUCT)
                    )
                    type.qualifiedName = "${file.fullRelativePath}$${statement.name.source}"
                    type.parentName = file.fullRelativePath
                    file.rootContext.addType(statement.name.source, type)
                    file.rootContext.addVariableOrError(api, statement.name.source, type, file.file, statement)
                }

                is TypeAliasStatement -> claim(file, entries, statement.identifier.source, statement)
                is EnumStatement -> {
                    if (!claim(file, entries, statement.name.source, statement)) continue
                    val id = DeclarationId(file.fullRelativePath, statement.name.source, DeclarationKind.ADT)
                    val names = statement.generics.map { it.source }
                    checkParameters(file, statement, names)
                    val declaration = AdtDeclaration(id, names.mapIndexed { i, name -> GenericType(name, id, i) })
                    file.rootContext.addType(statement.name.source, declaration.applied(declaration.parameters))
                }

                is DefStatement -> {
                    val function = DefType(
                        statement.name.source, arrayListOf(),
                        statement.generics.orEmpty().map { it.source }.toMutableList(),
                        DeclarationId(file.fullRelativePath, statement.name.source, DeclarationKind.FUNCTION)
                    )
                    statement.functionType = function
                    checkParameters(file, statement, function.generics)
                    if (file.rootContext.hasLocalVariable(statement.name.source)) {
                        RedeclarationException().send(api, file.file, statement, statement.name.source)
                    } else {
                        file.rootContext.addFunction(statement.name.source, function)
                    }
                    entries["def::${statement.name.source}::${entries.size}"] = statement
                }
            }
        }
    }

    private fun claim(file: IxFile, entries: MutableMap<String, Statement>, name: String, node: Statement): Boolean {
        if (name in entries || TypeUtils.getFromString(name) != null || name in setOf(
                "unit",
                "never",
                "Option",
                "Result"
            )
        ) {
            TypeDeclarationException().send(api, file.file, node, "duplicate or reserved type $name")
            return false
        }
        entries[name] = node
        return true
    }

    private fun checkParameters(file: IxFile, node: Statement, names: List<String>) {
        if (names.size != names.distinct().size || names.any { TypeUtils.getFromString(it) != null }) {
            TypeDeclarationException().send(api, file.file, node, "duplicate or reserved type parameter")
        }
    }

    private fun resolveStruct(file: IxFile, node: StructStatement) {
        val type = file.rootContext.getType(node.name.source) as? StructType ?: return
        val parameters =
            type.generics.mapIndexed { index, name -> name to GenericType(name, type.declarationId, index) }.toMap()
        checkFields(file, node, node.fields.map { it.name.source })
        type.parameters.addAll(node.fields.map { it.name.source to resolve(file, it.type!!, parameters) })
    }

    private fun resolveFunction(file: IxFile, node: DefStatement) {
        val function = node.functionType ?: return
        val parameters =
            function.generics.mapIndexed { index, name -> name to GenericType(name, function.declarationId, index) }
                .toMap()
        checkFields(file, node, node.parameters.orEmpty().map { it.name.source })
        function.parameters.addAll(
            node.parameters.orEmpty().map { it.name.source to resolve(file, it.type!!, parameters) })
        function.returnType = node.returnType?.let { resolve(file, it, parameters) } ?: BuiltInType.VOID
    }

    private fun checkFields(file: IxFile, node: Statement, names: List<String>) {
        if (names.distinct().size != names.size) {
            TypeDeclarationException().send(api, file.file, node, "duplicate field or parameter")
        }
    }

    private fun resolveAlias(file: IxFile, node: TypeAliasStatement): IxType {
        aliases[node]?.let { return it }
        if (node in visitingAliases) {
            val cycle = (visitingAliases.dropWhile { it !== node } + node).joinToString(" -> ") { it.identifier.source }
            TypeDeclarationException().send(api, file.file, node, "cyclic alias $cycle")
            return UnknownType(node.identifier.source)
        }
        visitingAliases.add(node)
        val id = DeclarationId(file.fullRelativePath, node.identifier.source, DeclarationKind.STRUCT)
        val parameters = node.generics.mapIndexed { i, name -> GenericType(name.source, id, i) }
        checkParameters(file, node, parameters.map { it.key })
        val body = resolve(file, node.typeStmt, parameters.associateBy { it.key })
        val type = if (parameters.isEmpty()) body else AliasType(parameters, body)
        visitingAliases.removeAt(visitingAliases.lastIndex)
        aliases[node] = type
        file.rootContext.addType(node.identifier.source, type)
        return type
    }

    private fun resolve(file: IxFile, node: TypeStatement, parameters: Map<String, GenericType> = emptyMap()): IxType {
        return TypeSyntax.resolve(node, { name, syntax -> parameters[name] ?: lookup(file, name, syntax) }, {
            TypeDeclarationException().send(api, file.file, node, it)
        })
    }

    private fun lookup(file: IxFile, name: String, node: TypeStatement): IxType {
        file.rootContext.getType(name)?.let { return it }
        if ("::" in name) {
            val moduleName = name.substringBefore("::")
            val member = name.substringAfter("::")
            val imported = file.imports.values.filterNotNull().find { it.name == moduleName }
            if (imported != null && imported.statements.any { it is ExportStatement && exportedName(it.stmt) == member }) {
                imported.rootContext.getType(member)?.let { return it }
                val alias = declarations[imported]?.get(member) as? TypeAliasStatement
                if (alias != null) return resolveAlias(imported, alias)
            }
        } else {
            val alias = declarations[file]?.get(name) as? TypeAliasStatement
            if (alias != null) return resolveAlias(file, alias)
        }
        IdentifierNotFoundException().send(api, file.file, node, name)
        return UnknownType(name)
    }

    private fun publishExports(file: IxFile) {
        file.exports.clear()
        file.exportedFunctions.clear()
        file.exportedTypes.clear()
        for (outer in file.statements.filterIsInstance<ExportStatement>()) {
            val name = exportedName(outer.stmt) ?: continue
            when (outer.stmt) {
                is StructStatement, is TypeAliasStatement, is EnumStatement -> file.rootContext.getType(name)?.let {
                    file.exportedTypes[name] = it
                }
            }
            if (outer.stmt is EnumStatement) {
                val type = file.rootContext.getType(name) as? AdtType
                type?.declaration?.variants?.forEach { variant ->
                    file.exports["$name::${variant.name}"] = file.rootContext.getVariable("$name::${variant.name}")
                }
            }
            if (outer.stmt !is TypeAliasStatement) {
                file.rootContext.getVariable(name)?.let { file.exports[name] = it }
            }
            if (outer.stmt is DefStatement) {
                val functions = file.rootContext.localFunctions()[name].orEmpty()
                if (functions.isNotEmpty()) file.exportedFunctions[name] = functions
            }
        }
    }

    private fun exportedName(node: Statement): String? = when (node) {
        is StructStatement -> node.name.source
        is EnumStatement -> node.name.source
        is TypeAliasStatement -> node.identifier.source
        is DefStatement -> node.name.source
        is VariableStatement -> node.name.source
        else -> null
    }
}
