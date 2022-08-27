package scontrino.proxy

import org.junit.jupiter.api.Test
import scontrino.util.Logging
import java.io.File
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

        val outputStream = File("build/dumpster/interactions.tsv")
            .also { it.parentFile.mkdirs() }
            .outputStream()
        val proxy = Proxy(host, proxiedPort, exposedPort, DelimitedInteractionLogger(outputStream))
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
