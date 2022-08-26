package scontrino.util

import java.net.Socket
import java.time.Instant
import java.util.Base64
import java.util.Date
import kotlin.concurrent.fixedRateTimer

fun spawn(task: () -> Unit): Thread = Thread { task() }.apply { start() }

fun encode64(buffer: ByteArray) = String(Base64.getEncoder().encode(buffer), Charsets.UTF_8)

fun Socket.show() = "${inetAddress.hostName}:$port"

fun Instant.toDate() = Date(toEpochMilli())

class Expirer(private val period: Long, private val onExpiration: () -> Unit) {
    private var lastAccessed: Instant = Instant.now()

    init {
        fixedRateTimer(
            period = period,
            startAt = lastAccessed.toDate(),
            action = {
                if (Instant.now().toEpochMilli() - lastAccessed.toEpochMilli() > period) {
                    onExpiration()
                    cancel()
                }
            }
        )
    }

    fun renew() {
        lastAccessed = Instant.now()
    }
}