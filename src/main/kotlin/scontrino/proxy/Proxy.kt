package scontrino.proxy

import scontrino.proxy.InteractionType.REQUEST
import scontrino.proxy.InteractionType.RESPONSE
import scontrino.util.Logging
import scontrino.util.ServerRunner
import scontrino.util.encode64
import scontrino.util.show
import scontrino.util.spawn
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.util.UUID

enum class InteractionType { REQUEST, RESPONSE }
class Interaction(
    val sessionId: UUID,
    val ipAddress: InetAddress,
    val type: InteractionType,
    val timestamp: Long,
    val payload: ByteArray
)

const val DefaultBufferSize: Int = 32 * 1024

class Proxy(
    val host: String,
    val proxiedPort: Int,
    val exposedPort: Int,
    val bufferSize: Int,
    private val interactionLogger: (Interaction) -> Unit = { _ -> }
) : ServerRunner(exposedPort) {

    constructor(
        host: String,
        proxiedPort: Int,
        exposedPort: Int,
        interactionLogger: (Interaction) -> Unit = { _ -> }
    ) : this(host, proxiedPort, exposedPort, DefaultBufferSize, interactionLogger)

    companion object : Logging

    override fun handle(clientSocket: Socket) {
        logger.debug("Proxying connection from ${clientSocket.show()}")

        spawn {
            val serverSocket = Socket(host, proxiedPort)
            logger.info("New connection ${clientSocket.show()} for ${serverSocket.show()}")

            val id: UUID = UUID.randomUUID()
            fun copy(inputStream: InputStream, outputStream: OutputStream, interactionType: InteractionType) {
                val buffer = ByteArray(bufferSize)
                generateSequence { inputStream.read(buffer) }
                    .takeWhile { byteCount -> byteCount >= 0 }
                    .forEach { byteCount ->
                        outputStream.write(buffer, 0, byteCount)
                        interactionLogger(Interaction(
                            id,
                            clientSocket.inetAddress,
                            interactionType,
                            System.currentTimeMillis(),
                            ByteArray(byteCount)
                                .also { buffer.copyInto(it, 0, 0, byteCount) }
                        ))
                    }
            }

            fun transcribe(inputSocket: Socket, outputSocket: Socket, type: InteractionType) =
                spawn { copy(inputSocket.inputStream, outputSocket.outputStream, type) }

            transcribe(clientSocket, serverSocket, REQUEST)
            transcribe(serverSocket, clientSocket, RESPONSE)
        }
    }
}

class DelimitedInteractionLogger(
    outputStream: OutputStream,
    private val delimiter: String = "\t"
) : (Interaction) -> Unit {

    private val out = PrintWriter(outputStream.writer(), true)

    override fun invoke(p1: Interaction) = out.println(
        listOf(
            p1.sessionId, p1.ipAddress,
            p1.type, p1.timestamp, encode64(p1.payload)
        )
            .joinToString(delimiter)
    )
}

