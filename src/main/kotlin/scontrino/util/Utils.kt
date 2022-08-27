package scontrino.util

import java.net.Socket
import java.util.Base64

fun spawn(task: () -> Unit): Thread = Thread { task() }.apply { start() }

fun encode64(buffer: ByteArray) = String(Base64.getEncoder().encode(buffer), Charsets.UTF_8)

fun Socket.show() = "${inetAddress.hostName}:$port"
