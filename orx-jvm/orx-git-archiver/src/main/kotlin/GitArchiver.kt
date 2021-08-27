import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants
import org.openrndr.AssetMetadata
import org.openrndr.Extension
import org.openrndr.Program

val logger = KotlinLogging.logger {  }
class GitArchiver : Extension {
    override var enabled: Boolean = true

    var commitOnRun = false
    var commitOnProduceAssets = true

    var autoCommitMessage = "auto commit"

    private val repo = FileRepository(".git")
    private val git = Git(repo)


    fun commitChanges() {
        git.add().addFilepattern("src").call()
        git.commit().setMessage(autoCommitMessage).call()
    }

    fun tag(name: String) : Boolean {
        val existing = git.tagList().call().find { it.name == name }
        if (existing != null) {
            git.tag().setName(name).call()
        } else {
            logger.warn { "tag $name exists" }
        }
        return existing != null
    }

    val commitHash:String
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
            logger.info { "current commit hash '$commitHash'" }
            AssetMetadata(oldMetadata.programName, "${oldMetadata.assetBaseName}$commitHash", program.assetProperties.mapValues { it.value })
        }

        program.produceAssets.listen {
            if (commitOnProduceAssets) {
                commitChanges()
            }
        }

        if (commitOnRun) {
            commitChanges()
        }
    }
}