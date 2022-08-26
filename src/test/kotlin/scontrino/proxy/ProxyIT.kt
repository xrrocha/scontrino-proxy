package scontrino.proxy

import org.junit.jupiter.api.Test
import scontrino.proxy.InteractionLogger.InteractionType
import scontrino.util.Logging
import kotlin.test.assertEquals

class ProxyIT {

    companion object: Logging

    @Test
    fun `proxy relays traffic`() {
        val host = "localhost"
        val proxiedPort = 9876
        val exposedPort = 6789
        val poisonPill = "bye, cruel world!"

        val server = LineServer(proxiedPort, poisonPill, String::uppercase)
            .also { it.start() }

        val interactionLogger = object : InteractionLogger {
            override fun logInteraction(timestamp: Long, interactionType: InteractionType, payload: ByteArray, bytes: Int) {
                logger.debug("Interaction log: $timestamp\t$interactionType\t${String(payload, 0, bytes)}")
            }
        }
        val proxy = Proxy(host, proxiedPort, exposedPort, interactionLogger)
            .also { it.start() }

        val client = LineClient(host, exposedPort, poisonPill)

        val words = listOf("la", "borsetta", "di", "mammà")
        val expectedWords = listOf("LA", "BORSETTA", "DI", "MAMMÀ")

        val actualWords = words.map(client::request)
        assertEquals(expectedWords, actualWords)

        client.close()
        server.stop()
        proxy.stop()
    }
}
