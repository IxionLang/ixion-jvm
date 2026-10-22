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

class LispExampleTest {
    @TempDir lateinit var temporary: Path

    // Every program gets a fresh Lisp environment. Multiform programs share it internally.
    private val cases = listOf(
        "" to "()",
        "; only a comment" to "()",
        "(+ 1 (* 2 3))" to "7",
        "(list (+) (*) (- 5) (- 10 2 3) (/ 20 2 2))" to "(0 1 -5 5 5)",
        "(list -2147483648 2147483647 +12)" to "(-2147483648 2147483647 12)",
        "'(a (b 2) #f)" to "(a (b 2) #f)",
        "''x" to "(quote x)",
        "(list \"a\\n\\t\\r\\\"\\\\b\" 'hello #t)" to "(\"a\\n\\t\\r\\\"\\\\b\" hello #t)",
        "(if #f missing 42)" to "42",
        "(if '() 1 missing)" to "1",
        "(if #t 7 (/ 1 0))" to "7",
        "(list (= 3 3) (< 1 2) (<= 2 2) (> 3 2) (>= 3 4) (not #f) (not 0))" to "(#t #t #t #t #f #t #f)",
        "(list (car '(a b)) (cdr '(a b)) (length '(a b)) (null? '()) (cons 1 '(2 3)))" to "(a (b) 2 #t (1 2 3))",
        "(list (equal? '(a (1)) '(a (1))) (equal? 'a \"a\") (equal? '(1) '(2)))" to "(#t #f #f)",
        "(define fact (lambda (n) (if (= n 0) 1 (* n (fact (- n 1)))))) (fact 6)" to "720",
        "(define make (lambda (x) (lambda (y) (+ x y)))) (define add (make 7)) ((lambda (x) (add 5)) 100)" to "12",
        "(define x 1) (define f (lambda () x)) (define x 9) (f)" to "9",
        "(define even (lambda (n) (if (= n 0) #t (odd (- n 1))))) (define odd (lambda (n) (if (= n 0) #f (even (- n 1))))) (even 8)" to "#t",
        "(define make (lambda () (define x 0) (lambda () (set! x (+ x 1)) x))) (define a (make)) (define b (make)) (list (a) (a) (b) (a))" to "(1 2 1 3)",
        "(define x 1) ((lambda (x) (set! x 8) x) 2) x" to "1",
        "(define f (lambda () (define x 1) (define g (lambda () x)) (set! x 8) g)) ((f))" to "8",
        "(define sum (lambda (xs) (if (null? xs) 0 (+ (car xs) (sum (cdr xs)))))) (sum '(1 2 3 4))" to "10",
        "(begin)" to "()",
        "(define + (lambda (a b) (* a b))) (+ 3 4)" to "12",
        "(+ 1" to "Error: Unclosed '('",
        ")" to "Error: Unexpected ')'",
        "'" to "Error: Expected expression",
        "\"unfinished" to "Error: Unterminated string",
        "\"bad\\q\"" to "Error: Unknown string escape: q",
        "\"bad\\" to "Error: Unterminated string escape",
        "2147483648" to "Error: Integer literal out of range: 2147483648",
        "-2147483649" to "Error: Integer literal out of range: -2147483649",
        "99999999999999999999" to "Error: Integer literal out of range: 99999999999999999999",
        "missing" to "Error: Unknown symbol: missing",
        "()" to "Error: Cannot call an empty list",
        "(1 2)" to "Error: Value is not callable",
        "(quote a b)" to "Error: quote expects 1 argument",
        "(if #t 1)" to "Error: if expects 3 arguments",
        "(lambda)" to "Error: lambda expects parameters and a body",
        "(lambda x 1)" to "Error: lambda expects a parameter list",
        "(lambda (x x) x)" to "Error: Duplicate parameter: x",
        "(lambda (1) 1)" to "Error: lambda parameters must be symbols",
        "((lambda (x) x))" to "Error: Wrong number of lambda arguments",
        "(define x)" to "Error: define expects 2 arguments",
        "(define 1 2)" to "Error: define expects a symbol",
        "(set! missing 1)" to "Error: Unknown symbol: missing",
        "(/ 1 0)" to "Error: Division by zero",
        "(/ 1)" to "Error: / has too few arguments",
        "(-)" to "Error: - has too few arguments",
        "(+ 1 #t)" to "Error: + expects numbers",
        "(= 1)" to "Error: = expects 2 numbers",
        "(< 1 'a)" to "Error: < expects numbers",
        "(cons 1 2)" to "Error: cons expects a list as its second argument",
        "(car '())" to "Error: car expects a nonempty list",
        "(cdr '())" to "Error: cdr expects a nonempty list",
        "(length 1)" to "Error: length expects a list",
        "(car '(1) '(2))" to "Error: car expects 1 argument",
        "(not)" to "Error: not expects 1 argument",
        "(equal? 1)" to "Error: equal? expects 2 arguments",
        "(begin missing (/ 1 0))" to "Error: Unknown symbol: missing",
        "((lambda (x) 42) missing)" to "Error: Unknown symbol: missing"
    )

    // Use a char literal for quotes: the current Ixion scanner ends strings at any quote.
    private fun ixString(text: String): String = text.split('"').joinToString(" + quoteChar() + ") { part ->
        "\"" + part.replace("\\", "\\\\").replace("\n", "\\n")
            .replace("\r", "\\r").replace("\t", "\\t") + "\""
    }

    @TestFactory fun interpreter_on_both_backends(): List<DynamicTest> = listOf(false, true).map { java ->
        DynamicTest.dynamicTest(if (java) "Lisp / Java" else "Lisp / ASM") {
            val root = Files.createTempDirectory(temporary, "lisp")
            val source = Files.readString(Path.of("examples/lisp.ix"))
                .replace("\ndef main() {", "\ndef showcaseMain() {")
            val main = cases.joinToString("\n", "\ndef main() {\n", "\n}\n") { (program, _) ->
                "    demo(${ixString(program)})"
            }
            Files.writeString(root.resolve("lisp.ix"), source + main)
            val api = IxApi()
            val entry = if (java) api.compileToJava(root.toString(), "lisp.ix", false)
                else api.compile(root.toString(), "lisp.ix", false)
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
                    assertEquals(cases.map { it.second }, capture.output!!.replace("\r\n", "\n").trim().lines())
                }
            }
        }
    }
}
