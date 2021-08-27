import org.openrndr.applicationSynchronous
import org.openrndr.extensions.Screenshots

fun main() = applicationSynchronous {
    program {
        val ga = extend(GitArchiver()) {
            commitOnRun = false
            commitOnProduceAssets = false
        }
        extend(Screenshots())
        extend {


        }
    }

}