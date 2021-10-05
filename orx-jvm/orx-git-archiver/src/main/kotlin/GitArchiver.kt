package org.openrndr.extra.gitarchiver

import mu.KotlinLogging
import org.openrndr.AssetMetadata
import org.openrndr.Extension
import org.openrndr.Program

internal interface GitProvider {
    fun commitChanges(commitMessage: String)
    fun headReference() : String
}

val logger = KotlinLogging.logger { }

class GitArchiver : Extension {
    override var enabled: Boolean = true

    var commitOnRun = false
    var commitOnRequestAssets = true

    var autoCommitMessage = "auto commit"

    private val git: GitProvider = if (nativeGitInstalled()) NativeGit() else JavaGit()

    override fun setup(program: Program) {
        val oldMetadataFunction = program.assetMetadata
        program.assetMetadata = {
            val oldMetadata = oldMetadataFunction()
            val commitHash = git.headReference()
            program.assetProperties["git-commit-hash"] = commitHash
            AssetMetadata(
                oldMetadata.programName,
                "${oldMetadata.assetBaseName}-$commitHash",
                program.assetProperties.mapValues { it.value })
        }

        program.requestAssets.listeners.add(0, {
            if (commitOnRequestAssets) {
                git.commitChanges(autoCommitMessage)
            }
        })

        if (commitOnRun) {
            git.commitChanges(autoCommitMessage)
        }
    }
}