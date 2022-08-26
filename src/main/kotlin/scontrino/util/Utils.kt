package scontrino.util

fun spawn(task: () -> Unit): Thread = Thread { task() }.apply { start() }
