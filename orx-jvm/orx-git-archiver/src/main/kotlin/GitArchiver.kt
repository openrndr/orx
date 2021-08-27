package org.openrndr.extra.gitarchiver

import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.EmptyCommitException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants
import org.openrndr.AssetMetadata
import org.openrndr.Extension
import org.openrndr.Program
import java.io.File

val logger = KotlinLogging.logger { }

class GitArchiver : Extension {
    override var enabled: Boolean = true

    var commitOnRun = false
    var commitOnRequestAssets = true

    var autoCommitMessage = "auto commit"

    private val repo = FileRepository(".git")
    private val git = Git.open(File("."))


    fun commitChanges() {
        try {
            git.commit().setAll(true).setAllowEmpty(false).setMessage(autoCommitMessage).call()
            logger.info { "git repository is now at ${commitHash.take(7)}" }
        } catch (e: EmptyCommitException) {
            logger.info { "no changes" }
        }
    }

    fun tag(name: String): Boolean {
        val existing = git.tagList().call().find { it.name == name }
        if (existing != null) {
            git.tag().setName(name).call()
        } else {
            logger.warn { "tag $name exists" }
        }
        return existing != null
    }

    val commitHash: String
        get() {
            val id = repo.resolve(Constants.HEAD)
            return id.name
        }

    override fun setup(program: Program) {
        val oldMetadataFunction = program.assetMetadata
        program.assetMetadata = {
            val oldMetadata = oldMetadataFunction()
            val commitHash = this.commitHash.take(7)
            program.assetProperties["git-commit-hash"] = commitHash
            AssetMetadata(
                oldMetadata.programName,
                "${oldMetadata.assetBaseName}-$commitHash",
                program.assetProperties.mapValues { it.value })
        }

        program.requestAssets.listeners.add(0, {
            if (commitOnRequestAssets) {
                commitChanges()
            }
        })


        if (commitOnRun) {
            commitChanges()
        }
    }
}