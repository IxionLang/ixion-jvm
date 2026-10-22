package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.AdtValue
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import com.kingmang.ixion.runtime.TupleValue
import com.kingmang.ixion.runtime.UnitValue
import com.kingmang.ixion.runtime.ixfunction.IxFunction1
import com.kingmang.ixion.typechecker.TypeTags
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
object HttpServerModule {
    private val pairTag = TypeTags.constant(HttpServerSignatures.pair)
    private val requestTag = TypeTags.constant(HttpServerSignatures.request)
    private const val RESULT_TAG = "<prelude>::Result[unit,string]"
    private val headerName = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
    private val managedHeaders = setOf("content-length", "transfer-encoding", "connection")

    @JvmStatic
    fun serve(
        host: String, port: Int, maxBodyBytes: Int, workers: Int,
        queueCapacity: Int, shutdownSeconds: Int, handler: IxFunction1<TupleValue, TupleValue>
    ): AdtValue {
        if (host.isBlank() || port !in 0..65535 || maxBodyBytes < 0 || maxBodyBytes == Int.MAX_VALUE ||
            workers <= 0 || queueCapacity <= 0 || shutdownSeconds < 0
        ) return failure("Invalid server configuration")

        var server: HttpServer? = null
        var executor: ThreadPoolExecutor? = null
        var shutdownHook: Thread? = null
        val stopped = AtomicBoolean(false)
        val finished = CountDownLatch(1)
        fun stop() {
            if (stopped.compareAndSet(false, true)) {
                try {
                    server?.stop(shutdownSeconds)
                } finally {
                    executor?.shutdownNow()
                    finished.countDown()
                }
            }
        }
        try {
            server = HttpServer.create(InetSocketAddress(host, port), queueCapacity)
            executor = ThreadPoolExecutor(
                workers, workers, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(queueCapacity),
                ThreadPoolExecutor.CallerRunsPolicy()
            )
            server.executor = executor
            server.createContext("/") { exchange -> handle(exchange, maxBodyBytes, handler) }
            shutdownHook = Thread({ stop() }, "ixion-http-shutdown")
            Runtime.getRuntime().addShutdownHook(shutdownHook)
            server.start()
            finished.await()
            return AdtValue(RESULT_TAG, "Ok", arrayOf(UnitValue))
        } catch (_: InterruptedException) {
            // Clear interruption during cleanup, then restore it for the caller.
            stop()
            Thread.currentThread().interrupt()
            return AdtValue(RESULT_TAG, "Ok", arrayOf(UnitValue))
        } catch (error: IOException) {
            return failure(error.message ?: "HTTP server I/O failure")
        } catch (error: IllegalArgumentException) {
            return failure(error.message ?: "Invalid server configuration")
        } finally {
            stop()
            shutdownHook?.let {
                try { Runtime.getRuntime().removeShutdownHook(it) } catch (_: IllegalStateException) { /* JVM shutdown */ }
            }
        }
    }

    private fun failure(message: String) = AdtValue(RESULT_TAG, "Err", arrayOf(message))

    private class BadRequest(val status: Int, message: String) : RuntimeException(message)

    private fun handle(exchange: HttpExchange, maxBodyBytes: Int, handler: IxFunction1<TupleValue, TupleValue>) {
        exchange.use {
            val response = try {
                val declaredLength = exchange.requestHeaders.getFirst("Content-Length")?.toLongOrNull()
                if (declaredLength != null && declaredLength > maxBodyBytes) throw BadRequest(413, "Payload Too Large")
                val bytes = exchange.requestBody.readNBytes(maxBodyBytes + 1)
                if (bytes.size > maxBodyBytes) throw BadRequest(413, "Payload Too Large")
                val body = try {
                    UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
                } catch (_: java.nio.charset.CharacterCodingException) {
                    throw BadRequest(400, "Invalid UTF-8 body")
                }
                val query = parseQuery(exchange.requestURI.rawQuery)
                val headers = exchange.requestHeaders.flatMap { (name, values) -> values.map { name to it } }
                val request = TupleValue(requestTag, arrayOf(
                    exchange.requestMethod, exchange.requestURI.path, pairs(query), pairs(headers), body
                ))
                validateResponse(requireNotNull(handler.apply(request)) { "Handler returned null" })
            } catch (error: BadRequest) {
                Response(error.status, listOf("Content-Type" to "text/plain; charset=UTF-8"), error.message!!)
            } catch (_: IOException) {
                return
            } catch (error: Exception) {
                System.err.println("HTTP handler failed: ${error.javaClass.simpleName}: ${error.message}")
                Response(500, listOf("Content-Type" to "text/plain; charset=UTF-8"), "Internal Server Error")
            }
            try {
                write(exchange, response)
            } catch (_: IOException) {
                // A disconnected client must not terminate the listener.
            }
        }
    }

    private data class Response(val status: Int, val headers: List<Pair<String, String>>, val body: String)

    private fun validateResponse(value: TupleValue): Response {
        val status = value.ixField(0) as Int
        require(status in 200..599) { "Response status must be between 200 and 599" }
        val headers = (value.ixField(1) as IxListWrapper).map {
            val pair = it as TupleValue
            val name = pair.ixField(0) as String
            val content = pair.ixField(1) as String
            require(headerName.matches(name)) { "Invalid response header name" }
            require(content.none { c -> c == '\r' || c == '\n' || c == '\u007f' || (c < ' ' && c != '\t') }) {
                "Invalid response header value"
            }
            require(name.lowercase(java.util.Locale.ROOT) !in managedHeaders) { "Transport-managed response header: $name" }
            name to content
        }
        return Response(status, headers, value.ixField(2) as String)
    }

    private fun write(exchange: HttpExchange, response: Response) {
        response.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
        val bytes = response.body.toByteArray(UTF_8)
        val head = exchange.requestMethod == "HEAD"
        val noBody = response.status == 204 || response.status == 205 || response.status == 304
        if (head && !noBody) exchange.responseHeaders.set("Content-Length", bytes.size.toString())
        if (response.status == 205) exchange.responseHeaders.set("Content-Length", "0")
        exchange.sendResponseHeaders(response.status, if (head || noBody || bytes.isEmpty()) -1 else bytes.size.toLong())
        if (!head && !noBody && bytes.isNotEmpty()) exchange.responseBody.write(bytes)
    }

    private fun pairs(values: List<Pair<String, String>>) = IxListWrapper(
        values.map { TupleValue(pairTag, arrayOf(it.first, it.second)) }, pairTag
    )

    private fun parseQuery(raw: String?): List<Pair<String, String>> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            raw.split('&').map { entry ->
                URLDecoder.decode(entry.substringBefore('='), UTF_8) to
                    URLDecoder.decode(entry.substringAfter('=', ""), UTF_8)
            }
        } catch (_: IllegalArgumentException) {
            throw BadRequest(400, "Invalid query encoding")
        }
    }
}
