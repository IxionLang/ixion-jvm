package com.kingmang.ixion.api

import com.kingmang.ixion.Visitor
import com.kingmang.ixion.ast.Statement
import com.kingmang.ixion.lexer.LexerImpl
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.runtime.IxType
import org.apache.commons.collections4.map.LinkedMap
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.nio.file.Path
import java.util.function.Function

class IxFile(projectRoot: String, relativePath: String, name: String?) {
    @JvmField
    val file: File
    @JvmField
    val statements: MutableList<Statement>
    @JvmField
    val exports: LinkedMap<String?, IxType?> = LinkedMap<String?, IxType?>()
    @JvmField
    val rootContext: Context = Context()
    @JvmField
    val projectRoot: String = FilenameUtils.separatorsToUnix(projectRoot)

    @JvmField
    val relativePath: String = FilenameUtils.separatorsToUnix(relativePath)

    @JvmField
    val name: String? = FilenameUtils.removeExtension(FilenameUtils.removeExtension(name))

    @JvmField
    val imports: LinkedMap<String?, IxFile?> = LinkedMap<String?, IxFile?>()

    init {

        val fullPath =
            FilenameUtils.separatorsToUnix(Path.of(projectRoot, relativePath, this.name + IxionConstant.EXT).toString())
        this.file = File(fullPath)

        val lexer = LexerImpl(file)
        val parser = Parser(lexer)
        this.statements = parser.parse()
    }

    fun <R> acceptVisitor(visitor: Visitor<out R?>?): MutableList<R?> {
        val results: MutableList<R?> = ArrayList<R?>()
        for (s in this.statements) {
            val r: R? = s.accept(visitor)
            results.add(r)
        }
        return results
    }

    fun addImport(absolute: String?, ixFile: IxFile?) {
        var absolute = absolute
        absolute = FilenameUtils.separatorsToUnix(absolute)
        imports[absolute] = ixFile
    }

    fun <T : Statement?, R> filter(kind: Class<out T?>, function: Function<in T?, R?>) {
        for (s in statements) {
            if (kind.isInstance(s)) {
                function.apply(kind.cast(s))
            }
        }
    }

    val fullRelativePath: String
        get() = FilenameUtils.separatorsToUnix(Path.of(relativePath, name).toString())

    override fun toString(): String {
        return file.getName()
    }
}
