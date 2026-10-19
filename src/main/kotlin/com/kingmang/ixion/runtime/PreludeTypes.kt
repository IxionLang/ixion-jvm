package com.kingmang.ixion.runtime

import com.kingmang.ixion.types.DeclarationId
import com.kingmang.ixion.types.DeclarationKind

/** Logical prelude identities are shared by source declarations and native signatures. */
object PreludeTypes {
    fun declaration(
        name: String,
        names: List<String>,
        variants: (List<GenericType>) -> List<AdtVariant>
    ): AdtDeclaration {
        val id = DeclarationId("<prelude>", name, DeclarationKind.ADT)
        val parameters = names.mapIndexed { i, parameter -> GenericType(parameter, id, i) }
        return AdtDeclaration(id, parameters).also { it.variants.addAll(variants(parameters)) }
    }

    val option = declaration("Option", listOf("T")) { p ->
        listOf(AdtVariant("None", emptyList()), AdtVariant("Some", listOf("value" to p[0])))
    }

    fun option(element: IxType): AdtType = option.applied(listOf(element))
}
