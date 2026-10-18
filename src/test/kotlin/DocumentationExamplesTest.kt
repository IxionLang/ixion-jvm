import com.kingmang.ixion.CombinedOutputCapture
import com.kingmang.ixion.api.IxApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider

/** The beginner guide is executable documentation: code and stdout come from Markdown. */
class DocumentationExamplesTest {
    @TempDir lateinit var temporary: Path

    @TestFactory fun beginner_guide_examples(): List<DynamicTest> {
        val guide = Files.readString(Path.of("docs/ru.md"))
        val modules = Regex("<!-- ixion-module: ([\\w/-]+) -->\\s*```ixion\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
            .findAll(guide).associate { it.groupValues[1] to it.groupValues[2] }
        val examples = Regex(
            "<!-- ixion-example: ([\\w-]+) -->\\s*```ixion\\n(.*?)\\n```\\s*Вывод:\\s*```text\\n(.*?)\\n```",
            RegexOption.DOT_MATCHES_ALL
        ).findAll(guide).toList()
        val markers = Regex("<!-- ixion-example: ([\\w-]+) -->").findAll(guide).count()
        assertTrue(markers >= 20, "The guide should include standalone programs")
        assertEquals(markers, examples.size, "Every runnable example needs a code block and expected stdout")
        assertEquals(markers, examples.map { it.groupValues[1] }.distinct().size, "Example names must be unique")
        assertEquals(markers + modules.size, Regex("```ixion\\n").findAll(guide).count(),
            "Every Ixion code block must be checked")
        return examples.flatMap { example ->
            listOf(false, true).map { java ->
                val name = example.groupValues[1]
                DynamicTest.dynamicTest("$name / ${if (java) "Java" else "ASM"}") {
                    val root = Files.createTempDirectory(temporary, name)
                    Files.writeString(root.resolve("program.ix"), example.groupValues[2])
                    modules.forEach { (module, code) ->
                        val path = root.resolve("$module.ix")
                        Files.createDirectories(path.parent)
                        Files.writeString(path, code)
                    }
                    val api = IxApi()
                    val entry = if (java) api.compileToJava(root.toString(), "program.ix", false)
                        else api.compile(root.toString(), "program.ix", false)
                    val output = root.resolve(".out")
                    if (java) {
                        val sources = Files.walk(output).use { paths -> paths.filter { it.toString().endsWith(".java") }.toList() }
                        val args = listOf("-classpath", System.getProperty("java.class.path"), "-d", output.toString()) + sources.map { it.toString() }
                        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, *args.toTypedArray()))
                    }
                    URLClassLoader(arrayOf(output.toUri().toURL()), javaClass.classLoader).use { loader ->
                        CombinedOutputCapture().use { capture ->
                            loader.loadClass(entry).getMethod("main", Array<String>::class.java)
                                .invoke(null, emptyArray<String>() as Any)
                            assertEquals(example.groupValues[3].trim(), capture.output!!.replace("\r\n", "\n").trim())
                        }
                    }
                }
            }
        }
    }
}
