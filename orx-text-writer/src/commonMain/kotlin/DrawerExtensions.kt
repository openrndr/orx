package org.openrndr.extra.textwriter

import org.openrndr.draw.Drawer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

@OptIn(ExperimentalContracts::class)
@JvmName("drawerWriter")
fun <T> Drawer.writer(f: TextWriter.() -> T): T {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    return writer(this, f)
}