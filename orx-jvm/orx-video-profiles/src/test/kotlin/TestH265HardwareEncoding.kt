import org.openrndr.extra.videoprofiles.H265Profile
import org.openrndr.ffmpeg.HDRTransfer
import org.openrndr.ffmpeg.ffmpegExecutable
import org.openrndr.ffmpeg.findHardwareEncoder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Encodes frames with the hardware HEVC encoder of this system, when there is one. These tests pass without encoding
 * on systems without a hardware encoder, like CI runners, and on macOS, where VideoToolbox is not probed.
 */
class TestH265HardwareEncoding {
    private val isMacOS = System.getProperty("os.name").lowercase().startsWith("mac")

    @Test
    fun `hardwareAcceleration encodes SDR frames`() {
        if (isMacOS) return
        val encoder = findHardwareEncoder("hevc") ?: return
        val profile = H265Profile().apply { hardwareAcceleration() }
        assertEquals(encoder.codec, profile.videoCodec)
        encode(profile, "rgba", 4)
    }

    @Test
    fun `hardwareAcceleration encodes HDR frames`() {
        if (isMacOS) return
        val encoder = findHardwareEncoder("hevc", "p010le") ?: return
        val profile = H265Profile().apply {
            hdr = HDRTransfer.PQ
            hardwareAcceleration()
        }
        assertEquals(encoder.codec, profile.videoCodec)
        encode(profile, "ayuv64le", 8)
    }

    @Test
    fun `hardwareAcceleration encodes full chroma frames`() {
        if (isMacOS) return
        val encoder = findHardwareEncoder("hevc", "yuv444p") ?: return
        val profile = H265Profile().apply {
            fullChroma = true
            hardwareAcceleration()
        }
        assertEquals(encoder.codec, profile.videoCodec)
        encode(profile, "rgba", 4)
    }

    /**
     * Encodes 10 frames of 256x256 pixels like VideoWriter does, with [bytesPerPixel] bytes per pixel in [inputFormat]
     */
    private fun encode(profile: H265Profile, inputFormat: String, bytesPerPixel: Int) {
        val width = 256
        val height = 256
        val output = File.createTempFile("orx-video-profiles", ".${profile.fileExtension}")
        val log = File.createTempFile("orx-video-profiles", ".txt")
        val command = listOf(
            ffmpegExecutable(), "-y", "-f", "rawvideo", "-vcodec", "rawvideo", "-s", "${width}x$height",
            "-pix_fmt", inputFormat, "-r", "25"
        ) + profile.inputArguments() + listOf("-i", "-") + profile.arguments() + output.path
        try {
            val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
            val frame = ByteArray(width * height * bytesPerPixel)
            process.outputStream.use { stream ->
                repeat(10) { index ->
                    frame.fill((index * 20).toByte())
                    stream.write(frame)
                }
            }
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "ffmpeg did not finish")
            assertEquals(0, process.exitValue(), "ffmpeg failed: ${command.joinToString(" ")}\n${log.readText()}")
            assertTrue(output.length() > 0, "no video was written")
        } finally {
            output.delete()
            log.delete()
        }
    }
}
