package org.openrndr.extra.envelopes

actual fun <V> mppSynchronized(lock: Any, f: () -> V): V {
    return f()
}