package scontrino.util

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

abstract class ServerRunner(port: Int) : Logging {
    private val thread = object : Thread() {
        private val serverSocket by lazy { ServerSocket(port) }
        override fun run() {
            generateSequence {
                try {
                    serverSocket.accept()
                } catch (_: IOException) {
                    null
                }
            }
                .forEach(::handle)
        }

        override fun interrupt() {
            try {
                serverSocket.close()
            } catch (_: IOException) {
            } finally {
                super.interrupt()
            }
        }
    }

    abstract fun handle(clientSocket: Socket)

    fun start() {
        logger.info("Starting server ${this::class.simpleName}")
        thread.start()
    }

    fun stop() {
        logger.info("Stopping ${this::class.simpleName}")
        thread.interrupt()
    }
}
