package scontrino.proxy

import org.slf4j.LoggerFactory
import java.net.Socket

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("scontrino.proxy.Main")

    val remoteHost = "localhost"
    val remotePort = 8081
    val localPort = 8080

    logger.info("Proxy server starting on port $localPort for remote $remoteHost:$remotePort")
    TcpIpProxy(remoteHost, remotePort, localPort).start()
}

class TcpIpProxy(host: String, proxiedPort: Int, exposedPort: Int) : ServerRunner(exposedPort) {
    val createConnection = { socket: Socket -> Connection(socket, host, proxiedPort) }
    override fun handle(socket: Socket) {
        logger.debug("Proxying connection from ${socket.show()}")
        Thread(createConnection(socket)).start()
    }
}

class Connection(private val clientSocket: Socket, host: String, port: Int) : Runnable {
    companion object : Logging

    private val serverSocket: Socket by lazy { Socket(host, port) }
    override fun run() {
        logger.info("New connection ${clientSocket.show()} for ${serverSocket.show()}")
        Thread(Proxy(clientSocket, serverSocket)).start()
        Thread(Proxy(serverSocket, clientSocket)).start()
    }
}

class Proxy(
    private val inputSocket: Socket,
    private val outputSocket: Socket,
    private val bufferSize: Int = 4096
) :
    Runnable {
    companion object : Logging

    override fun run() {
        logger.info("Proxy ${inputSocket.show()} --> ${outputSocket.show()}")
        inputSocket.use { inputSocket.inputStream.copyTo(outputSocket.outputStream, bufferSize) }
    }
}

interface Logging {
    val logger get() = LoggerFactory.getLogger(this::class.java)!!
}

fun Socket.show() = "${inetAddress.hostName}:$port"
