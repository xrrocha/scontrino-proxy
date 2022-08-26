package scontrino.proxy

import scontrino.proxy.Session.Interaction
import scontrino.proxy.Session.InteractionType
import scontrino.proxy.Session.InteractionType.REQUEST
import scontrino.proxy.Session.InteractionType.RESPONSE
import scontrino.util.Expirer
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

const val BufferSize: Int = 32 * 1024
const val SessionDuration = 30 * 60 * 1000L

class Session(
    val ipAddress: InetAddress,
    val initialTimestamp: Long = System.currentTimeMillis(),
    private val interactionLogger: (Session, Interaction) -> Unit = { _, _ -> }
) {

    val id: UUID = UUID.randomUUID()

    enum class InteractionType { REQUEST, RESPONSE }
    class Interaction(
        val type: InteractionType,
        val timestamp: Long,
        val payload: ByteArray,
        val offset: Int,
        val size: Int
    )

    fun append(type: InteractionType, buffer: ByteArray, offset: Int, size: Int) =
        Interaction(type, System.currentTimeMillis(), buffer, offset, size)
            .also { interactionLogger(this, it) }
}

class Proxy(
    val host: String,
    val proxiedPort: Int,
    val exposedPort: Int,
    private val interactionLogger: (Session, Interaction) -> Unit = { _, _ -> }
) : ServerRunner(exposedPort) {

    companion object : Logging

    private val sessions = mutableMapOf<InetAddress, Session>()

    override fun handle(clientSocket: Socket) {
        logger.debug("Proxying connection from ${clientSocket.show()}")

        spawn {

            val serverSocket = Socket(host, proxiedPort)
            logger.info("New connection ${clientSocket.show()} for ${serverSocket.show()}")

            val session = sessions.computeIfAbsent(clientSocket.inetAddress) {
                val expirer = Expirer(SessionDuration) {
                    sessions -= clientSocket.inetAddress
                }
                Session(clientSocket.inetAddress, System.currentTimeMillis()) { session, interaction ->
                    expirer.renew()
                    interactionLogger(session, interaction)
                }
            }

            fun copy(inputStream: InputStream, outputStream: OutputStream, type: InteractionType) {
                val buffer = ByteArray(BufferSize)
                generateSequence { inputStream.read(buffer) }
                    .takeWhile { byteCount -> byteCount >= 0 }
                    .forEach { byteCount ->
                        outputStream.write(buffer, 0, byteCount)
                        session.append(type, buffer, 0, byteCount)
                    }
                // TODO Is flushing really necessary?
                outputStream.flush()
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
) : (Session, Interaction) -> Unit {

    private val out = PrintWriter(outputStream.writer(), true)

    override fun invoke(p1: Session, p2: Interaction) = out.println(
        listOf(
            p1.id, p1.ipAddress, p1.initialTimestamp,
            p2.type, p2.timestamp, encode64(
                ByteArray(p2.size).also { p2.payload.copyInto(it, 0, p2.offset, p2.offset + p2.size) }
            )
        )
            .joinToString(delimiter)
    )
}

