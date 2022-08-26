package scontrino.proxy

import scontrino.proxy.InteractionLogger.InteractionType
import scontrino.proxy.InteractionLogger.InteractionType.REQUEST
import scontrino.proxy.InteractionLogger.InteractionType.RESPONSE
import scontrino.util.Logging
import scontrino.util.ServerRunner
import scontrino.util.spawn
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong

interface InteractionLogger {

    enum class InteractionType { REQUEST, RESPONSE }
    fun logInteraction(timestamp: Long, interactionType: InteractionType, payload: ByteArray, bytes: Int)

    object NoOp : InteractionLogger {
        override fun logInteraction(timestamp: Long, interactionType: InteractionType, payload: ByteArray, bytes: Int) {
        }
    }
}

class Proxy(
    private val host: String,
    private val proxiedPort: Int,
    exposedPort: Int,
    private val interactionLogger: InteractionLogger = InteractionLogger.NoOp,
    private val bufferSize: Int = 4096
) : ServerRunner(exposedPort) {

    companion object : Logging

    override fun handle(clientSocket: Socket) {
        logger.debug("Proxying connection from ${clientSocket.show()}")
        spawn {
            val serverSocket = Socket(host, proxiedPort)
            logger.info("New connection ${clientSocket.show()} for ${serverSocket.show()}")

            fun copy(inputStream: InputStream, outputStream: OutputStream, interactionType: InteractionType) {
                val buffer = ByteArray(bufferSize)
                val timestamp = System.currentTimeMillis()
                generateSequence { inputStream.read(buffer) }
                    .takeWhile { byteCount -> byteCount >= 0 }
                    .forEach { byteCount ->
                        outputStream.write(buffer, 0, byteCount)
                        interactionLogger.logInteraction(
                            timestamp, interactionType,
                            buffer, byteCount
                        )
                    }
                outputStream.flush()
            }

            fun transcribe(inputSocket: Socket, outputSocket: Socket, interactionType: InteractionType) =
                spawn {
                    inputSocket.use {
                        copy(inputSocket.inputStream, outputSocket.outputStream, interactionType)
                    }
                }

            transcribe(clientSocket, serverSocket, REQUEST)
            transcribe(serverSocket, clientSocket, RESPONSE)
        }
    }
}

fun Socket.show() = "${inetAddress.hostName}:$port"
