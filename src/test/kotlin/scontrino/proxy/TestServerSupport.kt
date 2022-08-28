package scontrino.proxy

import org.slf4j.LoggerFactory
import scontrino.util.Logging
import scontrino.util.ServerSocketRunner
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket

class LineIO(inputStream: InputStream, outputStream: OutputStream) {
    private val input = inputStream.bufferedReader()
    private val output = PrintWriter(outputStream.writer(), true)
    fun readLine(): String = input.readLine()!!
    fun println(string: String) = output.println(string)
}

fun Socket.asLineRW() = LineIO(inputStream, outputStream)

class LineServer(
    port: Int,
    private val poisonPill: String,
    private val transform: (String) -> String
) : ServerSocketRunner(port, { clientSocket ->
    val logger = LoggerFactory.getLogger(LineServer::class.java)
    logger.debug("Got connection")
    Thread {
        val lineIO = clientSocket.asLineRW()
        generateSequence { lineIO.readLine() }
            .takeWhile { it != poisonPill }
            .forEach { message ->
                logger.debug("Request: $message")
                lineIO.println(transform(message))
            }
        logger.debug("Closing client connection")
        clientSocket.close()
    }
        .also {
            logger.info("Starting server")
            it.start()
        }
})

class LineClient(host: String, port: Int, private val poisonPill: String) : Logging {
    private val socket by lazy { Socket(host, port) }
    private val lineIO by lazy { socket.asLineRW() }

    fun request(payload: String): String {
        logger.debug("Request: $payload")
        lineIO.println(payload)
        return lineIO.readLine().also {
            logger.debug("Response: $it")
        }
    }

    fun close() {
        lineIO.println(poisonPill)
        socket.close()
    }
}
