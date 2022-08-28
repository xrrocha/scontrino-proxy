package scontrino.util

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

open class ThreadRunner<T>(
    seed: () -> T,
    run: (T) -> Unit,
    private val wrapup: (T) -> Unit
) {
    private val seedValue by lazy { seed() }
    private val thread by lazy { Thread { run(seedValue) } }

    fun start() {
        thread.start()
    }

    fun stop() {
        try {
            wrapup(seedValue)
        } finally {
            thread.interrupt()
        }
    }
}

class ServerConfig(private val port: Int, private val pool: ExecutorService) {
    val serverSocket by lazy { ServerSocket(port) }
    fun start() = ServerSocket(port)
    fun stop() {
        pool.shutdown() // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) System.err.println("Pool did not terminate")
            }
        } catch (ex: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }
}
open class ServerSocketRunner(port: Int, handle: (Socket) -> Unit) : ThreadRunner<ServerSocket>(
    seed = { ServerSocket(port) },
    run = { serverSocket ->
        generateSequence {
            try {
                serverSocket.accept()
            } catch (_: IOException) {
                null
            }
        }
            .forEach(handle)
    },
    wrapup = { serverSocket -> serverSocket.close() }
)

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

    open fun start() {
        logger.info("Starting server ${this::class.simpleName}")
        thread.start()
    }

    open fun stop() {
        logger.info("Stopping ${this::class.simpleName}")
        thread.interrupt()
    }
}
