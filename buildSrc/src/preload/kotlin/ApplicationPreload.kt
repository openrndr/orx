package org.openrndr

import org.openrndr.ApplicationPreload
import org.openrndr.extensions.SingleScreenshot

class Preload : ApplicationPreload() {
    override fun onConfiguration(configuration: Configuration) {
//        configuration.width = 1280
//        configuration.height = 720
    }
    override fun onProgramSetup(program: Program) {
        println("installing single screenshot extension at 0")
        program.extensions.add(0, SingleScreenshot().apply {
            this.outputFile = System.getProperty("screenshotPath")
            setup(program)
        })
    }
}