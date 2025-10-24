package org.openrndr.extra.crashhandler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import java.io.File

private val logger = KotlinLogging.logger { }

class CrashHandler : Extension {
    override var enabled: Boolean = true

    var name: String? = null
    var vncHost: String? = null


    val reporters = mutableListOf<Reporter>()


    override fun setup(program: Program) {
        if (name == null)
            name = program.name

        Thread.setDefaultUncaughtExceptionHandler { t, e: Throwable ->
            logger.error(e) { "Uncaught exception in thread $t" }

            for (reporter in reporters) {
                try {
                    reporter.reportCrash(e)
                } catch (e: Exception) {
                    println("error while reporting")
                    logger.error(e) { "reporter threw an exception" }
                }
            }

            val crashFile = File("${program.name}.crash")
            val lastCrash = if (crashFile.exists()) crashFile.readText().toLongOrNull() ?: 0L else 0L

            crashFile.writeText("${System.currentTimeMillis()}")
            if (System.currentTimeMillis() - lastCrash < 60 * 1000) {
                logger.info { "crashed less than 60 seconds ago,  sleeping for 60 seconds" }
                Thread.sleep(60 * 1000L)
            }

            System.exit(1)
        }
    }

}