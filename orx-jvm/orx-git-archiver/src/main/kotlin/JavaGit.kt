package org.openrndr.extra.gitarchiver

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.EmptyCommitException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class JavaGit : GitProvider {
    private val repo = FileRepositoryBuilder().setGitDir(File("./.git")).build()
    private val git = Git(repo)

    override fun commitChanges(commitMessage: String) {
        try {
            git.commit().setAll(true).setAllowEmpty(false).setMessage(commitMessage).call()
            logger.info { "git repository is now at ${headReference()}" }
        } catch (e: EmptyCommitException) {
            logger.info { "no changes" }
        }
    }

    override fun headReference() : String {
        val id = repo.resolve(Constants.HEAD)
        return id.name.take(7)
    }

//    fun tag(name: String): Boolean {
//        val existing = git.tagList().call().find { it.name == name }
//        if (existing != null) {
//            git.tag().setName(name).call()
//        } else {
//            logger.warn { "tag $name exists" }
//        }
//        return existing != null
//    }
}