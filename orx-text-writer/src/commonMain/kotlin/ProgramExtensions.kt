package org.openrndr.extra.textwriter

import org.openrndr.Program
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T> Program.writer(f: TextWriter.() -> T): T {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    return writer(drawer, f)
}
