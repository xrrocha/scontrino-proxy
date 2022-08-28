package scontrino.proxy

import org.h2.tools.Server
import org.junit.jupiter.api.Test
import scontrino.util.Logging
import scontrino.util.ThreadRunner
import java.io.File
import java.util.Properties
import kotlin.test.assertEquals

class ProxyIT {

    companion object : Logging {
        val Dumpster = File("build/tmp/test/proxy/").also { it.mkdirs() }
    }

    @Test
    fun `proxy relays traffic`() {
        val host = "localhost"
        val proxiedPort = 9876
        val exposedPort = 6789
        val poisonPill = "bye, cruel world!"

        val upperCaseServer = LineServer(proxiedPort, poisonPill, String::uppercase)
            .also { it.start() }

        val proxy = Proxy(
            host, proxiedPort, exposedPort,
            DelimitedInteractionLogger(fileOSFor("uppercase"))
        )
            .also { it.start() }

        val upperCaseClient = LineClient(host, exposedPort, poisonPill)

        val words = listOf("la", "borsetta", "di", "mammà")
        val expectedWords = listOf("LA", "BORSETTA", "DI", "MAMMÀ")

        val actualWords = words.map(upperCaseClient::request)
        assertEquals(expectedWords, actualWords)

        upperCaseClient.close()
        upperCaseServer.stop()
        proxy.stop()
    }

    @Test
    fun `Relays H2`() {
        val host = "localhost"
        val proxiedPort = 9876
        val exposedPort = 6789

        val jdbcUrl = "jdbc:h2:tcp://localhost:$exposedPort/db"
        val jdbcProperties = Properties().also { props ->
            mapOf(
                "user" to "sa",
                "password" to "sa"
            )
                .forEach { (propertyName, propertyValue) ->
                    props.setProperty(propertyName, propertyValue)
                }
        }

        val threadRunner = ThreadRunner(
            seed = {
                Server.createTcpServer(
                    "-tcpPort", proxiedPort.toString(),
                    "-ifNotExists", "-baseDir", Dumpster.absolutePath
                )
            },
            run = { it.start() },
            wrapup = { it.stop() }
        )
            .also { it.start() }

        val proxy = Proxy(
            host, proxiedPort, exposedPort,
            DelimitedInteractionLogger(fileOSFor("h2"))
        )
            .also { it.start() }

        val connection = org.h2.Driver().connect(jdbcUrl, jdbcProperties)

        """
            CREATE TABLE IF NOT EXISTS language(
                code char(2) not null primary key,
                name varchar(16) not null
            );
            TRUNCATE TABLE language;
            INSERT INTO language VALUES('en', 'English');
            INSERT INTO language VALUES('es', 'Spanish');
            INSERT INTO language VALUES('de', 'German');
        """
            .split(";".toRegex())
            .forEach { sql ->
                connection.createStatement().execute(sql)
            }
    }

    private fun fileOSFor(baseName: String) =
        File(Dumpster, "$baseName-interactions.tsv").outputStream()
}
