package org.openrndr.extra.envelopes

expect fun <V> mppSynchronized(lock:Any, f:()->V) : V