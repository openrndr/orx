import org.openrndr.application
import org.openrndr.extra.minim.minim

fun main() {
    application {
        program {
            val minim = minim()
            val player = minim.loadFile(
                "demo-data/sounds/26777__junggle__btn402.mp3"
            )

            // fade gain to -40dB in 15 seconds
            player.shiftGain(player.gain, -40f, 15000)

            extend {
                if(frameCount % 30 == 0) {
                    player.rewind()
                    //player.gain = Random.nextDouble(-20.0, 0.0).toFloat()
                    player.play()
                }
            }
        }
    }
}