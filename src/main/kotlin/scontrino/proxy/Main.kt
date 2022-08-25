package scontrino.proxy

import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("scontrino.proxy.Main")

    val remoteHost = "localhost"
    val remotePort = 8081
    val localPort = 8080

    logger.info("Proxy server starting on port $localPort for remote $remoteHost:$remotePort")
    Proxy(remoteHost, remotePort, localPort).start()
}
