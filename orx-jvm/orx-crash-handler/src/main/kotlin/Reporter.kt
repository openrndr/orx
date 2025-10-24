package org.openrndr.extra.crashhandler

abstract class Reporter(val handler: CrashHandler) {
    abstract fun reportCrash(throwable: Throwable)
}