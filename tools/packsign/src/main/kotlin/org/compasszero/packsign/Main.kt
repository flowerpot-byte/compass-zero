package org.compasszero.packsign

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val code = Commands.run(args, System.out)
    if (code != 0) exitProcess(code)
}
