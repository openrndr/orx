import org.openrndr.extra.videoprofiles.ProresProfile
import org.openrndr.ffmpeg.ffmpegExecutable
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Encodes frames of colored blocks with ProRes and decodes them as a player does, following the color tags of the
 * video. The VideoToolbox encoder is tested on macOS only.
 */
class TestProresEncoding {
    private val isMacOS = System.getProperty("os.name").lowercase().startsWith("mac")

    private val width = 128
    private val height = 64

    /** the colors of the blocks, the top row is in the first frame row that is written */
    private val colors = listOf(
        listOf(220, 40, 40), listOf(40, 200, 60), listOf(30, 60, 210), listOf(128, 128, 128),
        listOf(250, 220, 30), listOf(20, 20, 20), listOf(240, 240, 240), listOf(160, 60, 200)
    )

    private fun blockOf(x: Int, y: Int) = (y / (height / 2)) * 4 + x / (width / 4)

    @Test
    fun `prores_ks encodes 422 colors tagged as BT 709`() = encode(ProresProfile(), "yuv422p10le")

    @Test
    fun `prores_ks encodes 4444 colors tagged as BT 709`() =
        encode(ProresProfile().apply { profile = ProresProfile.Profile.HQ4444 }, "yuva444p")

    @Test
    fun `prores_videotoolbox encodes 422 colors tagged as BT 709`() {
        if (!isMacOS) return
        val profile = ProresProfile().apply { hardwareAcceleration() }
        assertEquals("prores_videotoolbox", profile.codec)
        encode(profile, "yuv422p10le")
    }

    @Test
    fun `prores_videotoolbox encodes 4444 colors tagged as BT 709`() {
        if (!isMacOS) return
        val profile = ProresProfile().apply {
            profile = ProresProfile.Profile.HQ4444
            hardwareAcceleration()
        }
        assertEquals("prores_videotoolbox", profile.codec)
        encode(profile, "yuva444p")
    }

    /**
     * Encodes 5 frames like VideoWriter does, in RGBA, and checks the [expectedPixelFormat] (a prefix), the color
     * tags and the colors of the blocks of the decoded video. The profile flips the frames vertically.
     */
    private fun encode(profile: ProresProfile, expectedPixelFormat: String) {
        val output = File.createTempFile("orx-video-profiles", ".${profile.fileExtension}")
        val log = File.createTempFile("orx-video-profiles", ".txt")
        val command = listOf(
            ffmpegExecutable(), "-y", "-f", "rawvideo", "-vcodec", "rawvideo", "-s", "${width}x$height",
            "-pix_fmt", "rgba", "-r", "25"
        ) + profile.inputArguments() + listOf("-i", "-") + profile.arguments() + output.path
        try {
            val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
            val frame = ByteArray(width * height * 4)
            for (y in 0 until height) for (x in 0 until width) {
                val color = colors[blockOf(x, y)]
                for (c in 0 until 3) frame[(y * width + x) * 4 + c] = color[c].toByte()
                frame[(y * width + x) * 4 + 3] = 255.toByte()
            }
            process.outputStream.use { stream -> repeat(5) { stream.write(frame) } }
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "ffmpeg did not finish")
            assertEquals(0, process.exitValue(), "ffmpeg failed: ${command.joinToString(" ")}\n${log.readText()}")

            val stream = ProcessBuilder(ffmpegExecutable(), "-hide_banner", "-i", output.path)
                .redirectErrorStream(true).start().inputStream.bufferedReader().readText()
                .lines().first { "Video:" in it }
            assertTrue(expectedPixelFormat in stream, "unexpected pixel format: $stream")
            assertTrue("(tv, bt709, progressive)" in stream, "the video is not tagged as BT.709: $stream")

            // decodes the first frame to RGBA, converted with the matrix of the color tags. Accurate rounding keeps
            // the conversion of 10 bit 4:2:2 from darkening the colors by a few levels.
            val decoder = ProcessBuilder(
                ffmpegExecutable(), "-v", "error", "-i", output.path, "-frames:v", "1",
                "-vf", "scale=flags=accurate_rnd+full_chroma_int,format=rgba", "-f", "rawvideo", "-pix_fmt", "rgba", "-"
            ).start()
            val decoded = decoder.inputStream.readBytes()
            assertTrue(decoder.waitFor(60, TimeUnit.SECONDS), "ffmpeg did not finish decoding")
            assertEquals(width * height * 4, decoded.size, "unexpected size of the decoded frame")

            // compares the centers of the blocks, away from the chroma of the edges between blocks
            for (block in colors.indices) {
                val bx = (block % 4) * (width / 4)
                val by = (1 - block / 4) * (height / 2) // flipped vertically
                val error = DoubleArray(4)
                var count = 0
                for (y in by + 8 until by + height / 2 - 8) for (x in bx + 8 until bx + width / 4 - 8) {
                    val pixel = (y * width + x) * 4
                    for (c in 0 until 3) error[c] += abs((decoded[pixel + c].toInt() and 0xff) - colors[block][c])
                    error[3] += abs((decoded[pixel + 3].toInt() and 0xff) - 255)
                    count++
                }
                for (c in 0 until 4) {
                    assertTrue(
                        error[c] / count < 2.0,
                        "${profile.codec} ${profile.profile}: block $block channel $c is off by " +
                                "${error[c] / count} on average, $stream"
                    )
                }
            }
        } finally {
            output.delete()
            log.delete()
        }
    }
}
