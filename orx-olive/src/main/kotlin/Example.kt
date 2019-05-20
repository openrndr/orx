package org.openrndr.extra.olive

import org.openrndr.Program
import org.openrndr.application

class StupidProgram:Program() {
    val thisIsStupid = 5
}

fun main() = application{
    program(StupidProgram()) {
        extend(Olive<StupidProgram>())

    }
}