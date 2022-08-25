package scontrino.proxy

import org.slf4j.LoggerFactory

interface Logging {
    val logger get() = LoggerFactory.getLogger(this::class.java)!!
}