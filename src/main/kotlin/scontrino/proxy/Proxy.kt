package scontrino.proxy

import scontrino.proxy.InteractionType.REQUEST
import scontrino.proxy.InteractionType.RESPONSE
import scontrino.util.Logging
import scontrino.util.ServerRunner
import scontrino.util.spawn
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

enum class InteractionType { REQUEST, RESPONSE }

class Interaction(
    val timestamp: Long,
    val interactionId: Int,
    val interactionType: InteractionType,
    val payload: ByteArray
) {

    fun toDelimited(separator: String = "\t") =
        listOf(timestamp, interactionId, interactionType, String(payload)).joinToString(separator)

    constructor(
        timestamp: Long,
        interactionId: Int,
        interactionType: InteractionType,
        payload: ByteArray,
        size: Int
    ) : this(
        timestamp,
        interactionId,
        interactionType,
        ByteArray(size).apply { payload.copyInto(this, 0, 0, size) })
}

const val BufferSize: Int = 4096

class Proxy(
    private val host: String,
    private val proxiedPort: Int,
    exposedPort: Int,
    private val logInteraction: (Interaction) -> Unit = { _ -> }
) : ServerRunner(exposedPort) {

    companion object : Logging

    private val interactionSequence = AtomicInteger(0)

    override fun handle(clientSocket: Socket) {
        logger.debug("Proxying connection from ${clientSocket.show()}")
        spawn {
            val timestamp = System.currentTimeMillis()
            val interactionId = interactionSequence.incrementAndGet()

            val serverSocket = Socket(host, proxiedPort)
            logger.info("New connection ${clientSocket.show()} for ${serverSocket.show()}")

            fun copy(inputStream: InputStream, outputStream: OutputStream, interactionType: InteractionType) {
                val buffer = ByteArray(BufferSize)
                generateSequence { inputStream.read(buffer) }
                    .takeWhile { byteCount -> byteCount >= 0 }
                    .forEach { byteCount ->
                        outputStream.write(buffer, 0, byteCount)
                        val interaction = Interaction(timestamp, interactionId, interactionType, buffer, byteCount)
                        logInteraction(interaction)
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
