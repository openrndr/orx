package org.openrndr.extra.gitarchiver

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val dir = File(".")

class NativeGit : GitProvider {
    override fun commitChanges(commitMessage: String) {
        val gitStatus = "git status --porcelain".runCommand(dir)!!
        if (gitStatus.first.isNotBlank()){
            if (gitStatus.first.contains("Not a git repository")){
                logger.error { "Can't commit changes because the working directory is not a git repository" }
            } else {
                "git add .".runCommand(dir)
                "git commit -m \"${commitMessage}\"".runCommand(dir)
                logger.info { "git repository is now at ${headReference()}" }
            }
        } else {
            logger.info { "no changes" }
        }
    }

    override fun headReference(): String {
        return "git rev-parse --short HEAD".runCommand(dir)!!.first.trimEnd()
    }
}

internal fun nativeGitInstalled(): Boolean {
    return "git --version".runCommand(dir) != null
}

// Adapted from https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
private fun String.runCommand(workingDir: File): Pair<String, String>? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return Pair(proc.inputStream.bufferedReader().readText(), proc.errorStream.bufferedReader().readText())
    } catch(e: IOException) {
        return null
    }
}