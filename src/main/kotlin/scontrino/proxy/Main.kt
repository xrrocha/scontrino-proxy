package scontrino.proxy

import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {
    val arguments = parseArgs(args.toList())
    val remoteHost = arguments.getOrDefault("host", "localhost")
    val remotePort = arguments.getOrDefault("remote-port", "8081").toInt()
    val localPort = arguments.getOrDefault("local-port", "8080").toInt()
    val dumpFileName = arguments.getOrDefault("dump-file", "./proxied-interactions.tsv")
    val bufferSize = arguments.getOrDefault("buffer-size", "4096").toInt()

    val logger = LoggerFactory.getLogger("scontrino.proxy.Main")

    val dumpFile = File(dumpFileName)
    logger.info("Starting proxy on for $remoteHost:$remotePort on port $localPort. Dump file: ${dumpFile.canonicalPath}")
    Proxy(
        remoteHost,
        remotePort,
        localPort,
        bufferSize,
        DelimitedInteractionLogger(dumpFile))
        .start()
}

fun parseArgs(args: Iterable<String>) =
    args
        .filter { it.startsWith("--") && it.contains("=") }
        .associate {
            val pos = it.indexOf('=')
            val name = it.substring(2, pos)
            val value = it.substring(pos + 1)
            Pair(name, value)
        }
