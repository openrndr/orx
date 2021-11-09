package org.openrndr.extra.gitarchiver

import mu.KotlinLogging
import org.openrndr.AssetMetadata
import org.openrndr.Extension
import org.openrndr.Program

interface GitProvider {
    fun commitChanges(commitMessage: String)
    fun headReference(): String
    fun logReferences(count: Int): List<String>
    fun show(reference: String) : String

    companion object {
        fun create() : GitProvider {
            return if (nativeGitInstalled()) NativeGit() else JavaGit()
        }
    }
}

val logger = KotlinLogging.logger { }

class GitArchiver : Extension {
    override var enabled: Boolean = true

    var commitOnRun = false
    var commitOnRequestAssets = true

    var autoCommitMessage = "auto commit"

    private val git: GitProvider = GitProvider.create()

    override fun setup(program: Program) {
        logger.info {
            "Using ${
                when (git) {
                    is NativeGit -> "native Git"
                    is JavaGit -> "Java Git"
                    else -> "unknown Git"
                }
            }"
        }

        autoCommitMessage = "auto commit from ${program.name}"

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