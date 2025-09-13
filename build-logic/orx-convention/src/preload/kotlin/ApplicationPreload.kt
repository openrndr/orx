package org.openrndr

import org.openrndr.extensions.SingleScreenshot

/**
 * This [Preload] class is used by the [CollectScreenshots] task to inject the [SingleScreenshot] extension
 */
class Preload : ApplicationPreload() {
    override fun onProgramSetup(program: Program) {
        program.extend(SingleScreenshot()) {
            this.outputFile = System.getProperty("screenshotPath")
        }
    }
}