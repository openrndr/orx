import org.openrndr.application
import org.openrndr.extra.crashhandler.CrashHandler
import org.openrndr.extra.crashhandler.slack

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            extend(CrashHandler()) {
                name = "jump-scare"
                vncHost = "localhost"
                slack {
                    authToken = System.getenv("SLACK_AUTH_TOKEN")
                    channelId = System.getenv("SLACK_CHANNEL_ID")
                }
            }

            extend {
                error("something bad happened")
            }
        }
    }
}