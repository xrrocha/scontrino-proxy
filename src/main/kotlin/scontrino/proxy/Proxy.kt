package scontrino.proxy

import java.net.Socket

class Proxy(host: String, proxiedPort: Int, exposedPort: Int) : ServerRunner(exposedPort) {
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
        Thread(Exchange(clientSocket, serverSocket)).start()
        Thread(Exchange(serverSocket, clientSocket)).start()
    }
}

class Exchange(
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

fun Socket.show() = "${inetAddress.hostName}:$port"
