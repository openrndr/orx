import org.openrndr.extra.videoprofiles.H265Profile
import org.openrndr.ffmpeg.HDRTransfer
import org.openrndr.ffmpeg.VideoRotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestH265Profile {
    private fun Array<String>.valueOf(option: String) = this[indexOf(option) + 1]

    @Test
    fun `arguments can be requested more than once`() {
        val profile = H265Profile()
        val first = profile.arguments()
        val second = profile.arguments()
        assertEquals(first.toList(), second.toList())
        assertEquals("vflip,colorspace=bt709:iall=bt601-6-625:fast=1", second.valueOf("-vf"))
        assertEquals(listOf("vflip"), profile.filters)
    }

    @Test
    fun `libx265 encodes with a constant rate factor`() {
        val profile = H265Profile().apply {
            constantRateFactor = 20
            preset = "slow"
        }
        val arguments = profile.arguments()
        assertEquals("libx265", arguments.valueOf("-vcodec"))
        assertEquals("20", arguments.valueOf("-crf"))
        assertEquals("slow", arguments.valueOf("-preset"))
        assertEquals("hvc1", arguments.valueOf("-tag:v"))
    }

    @Test
    fun `rotation is applied after flipping`() {
        val profile = H265Profile().apply { rotation = VideoRotation.CLOCKWISE_90 }
        assertEquals("vflip,transpose=clock,colorspace=bt709:iall=bt601-6-625:fast=1", profile.arguments().valueOf("-vf"))
    }

    @Test
    fun `VideoToolbox encodes with a constant quality`() {
        val profile = H265Profile().apply { videoCodec = CODEC_HEVC_VIDEOTOOLBOX }
        val arguments = profile.arguments()
        assertEquals("hevc_videotoolbox", arguments.valueOf("-vcodec"))
        assertEquals("1", arguments.valueOf("-allow_sw"))
        assertEquals("55", arguments.valueOf("-q:v"))
        assertFalse("-crf" in arguments)
    }

    @Test
    fun `NVENC encodes with a constant quantizer and full chroma`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_NVENC
            fullChroma = true
            constantRateFactor = 20
        }
        val arguments = profile.arguments()
        assertEquals("hevc_nvenc", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertEquals("p5", arguments.valueOf("-preset"))
        assertEquals("25", arguments.valueOf("-cq"))
        assertEquals("0", arguments.valueOf("-b:v"))
        assertFalse("-crf" in arguments)
        assertFalse("hwupload" in arguments.valueOf("-vf"))
    }

    @Test
    fun `NVENC encodes HDR video as P010`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_NVENC
            hdr = HDRTransfer.PQ
        }
        val arguments = profile.arguments()
        assertEquals("hevc_nvenc", arguments.valueOf("-vcodec"))
        assertEquals("p010le", arguments.valueOf("-pix_fmt"))
        assertEquals("vflip,format=p010le", arguments.valueOf("-vf"))
        assertEquals("smpte2084", arguments.valueOf("-color_trc"))
        assertEquals("25", arguments.valueOf("-cq"))
        assertFalse("-x265-params" in arguments)
        assertFalse("-profile:v" in arguments)
    }

    @Test
    fun `NVENC encodes full chroma HDR video with 16 bits per component`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_NVENC
            hdr = HDRTransfer.HLG
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("hevc_nvenc", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p16le", arguments.valueOf("-pix_fmt"))
        assertEquals("vflip,format=yuv444p16le", arguments.valueOf("-vf"))
    }

    @Test
    fun `VAAPI encodes frames uploaded as NV12`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_VAAPI
            vaapiDevice = "/dev/dri/renderD129"
            quality = 100
        }
        val arguments = profile.arguments()
        assertEquals("hevc_vaapi", arguments.valueOf("-vcodec"))
        assertEquals("/dev/dri/renderD129", arguments.valueOf("-vaapi_device"))
        assertTrue(arguments.valueOf("-vf").endsWith(",format=nv12,hwupload"))
        assertFalse("-pix_fmt" in arguments)
        assertEquals("CQP", arguments.valueOf("-rc_mode"))
        assertEquals("4", arguments.valueOf("-qp"))
        assertEquals("hvc1", arguments.valueOf("-tag:v"))
    }

    @Test
    fun `VAAPI encodes HDR frames uploaded as P010`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_VAAPI
            hdr = HDRTransfer.PQ
        }
        val arguments = profile.arguments()
        assertEquals("hevc_vaapi", arguments.valueOf("-vcodec"))
        assertEquals("vflip,format=p010le,hwupload", arguments.valueOf("-vf"))
        assertFalse("-pix_fmt" in arguments)
        assertEquals("/dev/dri/renderD128", arguments.valueOf("-vaapi_device"))
        assertEquals("25", arguments.valueOf("-qp"))
        assertEquals("bt2020", arguments.valueOf("-color_primaries"))
        assertFalse("-x265-params" in arguments)
    }

    @Test
    fun `VAAPI full chroma is encoded with libx265`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_VAAPI
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("libx265", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertFalse("hwupload" in arguments.valueOf("-vf"))
        assertFalse("-vaapi_device" in arguments)
    }

    @Test
    fun `VAAPI full chroma HDR video is encoded with libx265`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_VAAPI
            fullChroma = true
            hdr = HDRTransfer.PQ
        }
        val arguments = profile.arguments()
        assertEquals("libx265", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p10le", arguments.valueOf("-pix_fmt"))
        assertTrue(arguments.valueOf("-x265-params").contains("hdr10=1"))
    }

    @Test
    fun `Quick Sync encodes NV12 with a constant quality`() {
        val profile = H265Profile().apply { videoCodec = CODEC_HEVC_QSV }
        val arguments = profile.arguments()
        assertEquals("hevc_qsv", arguments.valueOf("-vcodec"))
        assertEquals("nv12", arguments.valueOf("-pix_fmt"))
        assertEquals("slow", arguments.valueOf("-preset"))
        assertEquals("25", arguments.valueOf("-global_quality"))
        assertFalse("-crf" in arguments)
    }

    @Test
    fun `Quick Sync encodes HDR video as P010`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_QSV
            hdr = HDRTransfer.PQ
        }
        val arguments = profile.arguments()
        assertEquals("hevc_qsv", arguments.valueOf("-vcodec"))
        assertEquals("p010le", arguments.valueOf("-pix_fmt"))
        assertEquals("vflip,format=p010le", arguments.valueOf("-vf"))
        assertEquals("smpte2084", arguments.valueOf("-color_trc"))
        assertEquals("25", arguments.valueOf("-global_quality"))
        assertFalse("-x265-params" in arguments)
        // Quick Sync selects the Main 10 profile for P010 frames
        assertFalse("-profile:v" in arguments)
    }

    @Test
    fun `AMF encodes HEVC without a B-frame quantizer`() {
        val profile = H265Profile().apply { videoCodec = CODEC_HEVC_AMF }
        val arguments = profile.arguments()
        assertEquals("hevc_amf", arguments.valueOf("-vcodec"))
        assertEquals("nv12", arguments.valueOf("-pix_fmt"))
        assertEquals("cqp", arguments.valueOf("-rc"))
        assertEquals("25", arguments.valueOf("-qp_i"))
        assertEquals("25", arguments.valueOf("-qp_p"))
        assertFalse("-qp_b" in arguments)
    }

    @Test
    fun `Media Foundation encodes with its hardware encoder at the quality`() {
        val profile = H265Profile().apply { videoCodec = CODEC_HEVC_MF }
        val arguments = profile.arguments()
        assertEquals("hevc_mf", arguments.valueOf("-vcodec"))
        assertEquals("1", arguments.valueOf("-hw_encoding"))
        assertEquals("quality", arguments.valueOf("-rate_control"))
        assertEquals("55", arguments.valueOf("-quality"))
    }

    @Test
    fun `Quick Sync full chroma is encoded with libx265`() {
        val profile = H265Profile().apply {
            videoCodec = CODEC_HEVC_QSV
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("libx265", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertFalse("-global_quality" in arguments)
    }

    @Test
    fun `quality null encodes at the default bit rate of the hardware encoders`() {
        val nvenc = H265Profile().apply { videoCodec = CODEC_HEVC_NVENC; quality = null }.arguments()
        assertFalse("-cq" in nvenc)
        assertFalse("-rc" in nvenc)
        val vaapi = H265Profile().apply { videoCodec = CODEC_HEVC_VAAPI; quality = null }.arguments()
        assertFalse("-qp" in vaapi)
        assertFalse("-rc_mode" in vaapi)
    }
}
