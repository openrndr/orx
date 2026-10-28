package org.openrndr.extra.videoprofiles

import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType

/**
 * True when running on macOS, where FFmpeg's VideoToolbox encoders use the hardware video encoders
 */
internal val isMacOS = System.getProperty("os.name").lowercase().startsWith("mac")

/**
 * Is [codec] one of FFmpeg's VideoToolbox (macOS) encoders?
 */
internal fun isVideoToolbox(codec: String?) = codec?.endsWith("_videotoolbox") == true

/**
 * Arguments for VideoToolbox encoders: fall back to software encoding when the hardware does not support the codec
 */
internal val videoToolboxArguments = arrayOf("-allow_sw", "1")
