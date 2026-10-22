import com.kingmang.ixion.CombinedOutputCapture
import com.kingmang.ixion.api.IxApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider

/** Run the actual showcase files with both backends, without network or user input. */
class ShowcaseExamplesTest {
    @TempDir lateinit var temporary: Path

    @TestFactory fun showcase_examples(): List<DynamicTest> {
        val expected = mapOf(
            "quicksort" to "[-2, 1, 3, 3, 5, 8, 9]\n[8, 3, 5, 3, -2, 9, 1]\n[]",
            "primes" to "[2, 3, 5, 7, 11, 13, 17, 19, 23, 29]\n129",
            "expression_tree" to "((2 + 5) * 6)\n42",
            "generic_tree" to "[3, 7, 12]\n[false, true, true]",
            "shopping_cart" to "[Notebook, Pen]\nTotal cents: 1450\nDiscounted cents: 1305\nOriginal cents: 1450",
            "state_machine" to "Playing: Midnight Drive\nPaused: Midnight Drive\nPlaying: Midnight Drive\nStopped",
            "run_length_encoding" to "4 x 3\n2 x 2\n7 x 1\n4 x 2\n0",
            "bank_transfer" to "Alice: 700\nBob: 500\nRejected: Insufficient funds\nRejected: Amount must be positive\nOriginal Alice: 1000",
            "collatz" to "[6, 3, 10, 5, 16, 8, 4, 2, 1]\nStep budget exhausted\nStart must be positive\nInteger overflow avoided",
            "json_config" to "Hello, Ixion!\nHello, guest!\nHello, guest!\nInvalid JSON\n{\"name\":\"Ixion\"}"
        )
        return expected.flatMap { (name, stdout) ->
            listOf(false, true).map { java ->
                DynamicTest.dynamicTest("$name / ${if (java) "Java" else "ASM"}") {
                    val root = Files.createTempDirectory(temporary, name)
                    Files.copy(Path.of("examples/$name.ix"), root.resolve("$name.ix"))
                    val api = IxApi()
                    val entry = if (java) api.compileToJava(root.toString(), "$name.ix", false)
                        else api.compile(root.toString(), "$name.ix", false)
                    val output = root.resolve(".out")
                    if (java) {
                        val sources = Files.walk(output).use { paths ->
                            paths.filter { it.toString().endsWith(".java") }.toList()
                        }
                        val args = listOf("-classpath", System.getProperty("java.class.path"), "-d", output.toString()) + sources.map { it.toString() }
                        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, *args.toTypedArray()))
                    }
                    URLClassLoader(arrayOf(output.toUri().toURL()), javaClass.classLoader).use { loader ->
                        CombinedOutputCapture().use { capture ->
                            loader.loadClass(entry).getMethod("main", Array<String>::class.java)
                                .invoke(null, emptyArray<String>() as Any)
                            assertEquals(stdout, capture.output!!.replace("\r\n", "\n").trim())
                        }
                    }
                }
            }
        }
    }
}
