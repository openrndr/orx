# orx-video-profiles

GIF, H265, PNG, Prores, TIFF and Webp `VideoWriterProfile`s for `ScreenRecorder` and `VideoWriter`.

## Usage

### GIF

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            gif()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

Then use `gifsicle` or a similar tool to reduce the gif file size. For example:

```bash
$ gifsicle --loop --delay=4 --colors 16 --optimize=2 heavy.gif >lessheavy.gif
```

More about [gifsicle](http://www.lcdf.org/gifsicle/).

### PNG sequence

This profile requires specifying a file name: `outputFile = "frame-%05d.png"`,
where `%05d` means "zero-padded five-digit frame number".
The frame number format is not optional.

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            pngSequence()
            outputFile = "frame-%05d.png"
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

### TIFF sequence

TIFF images are lossless and they can be faster to write than other formats. You will probably need to convert
them later to a more common format or combine them into a video file.

This profile requires specifying a file name: `outputFile = "frame-%05d.tif"`,
where `%05d` means "zero-padded five-digit frame number".
The frame number format is not optional.

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            tiffSequence()
            outputFile = "frame-%05d.tif"
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```


### Animated Webp

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            webp()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```


### Prores (large file, high quality video)

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            prores {
                profile = ProresProfile.Profile.HQ4444
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

### H265

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            h265 {
                constantRateFactor = 23
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

#### Hardware encoding

`hardwareAcceleration()` encodes with the hardware HEVC encoder of the system, which uses a fraction of the CPU
time of `libx265`: VideoToolbox on macOS, on Linux NVENC on NVIDIA GPUs or VAAPI on Intel and AMD GPUs, and on
Windows NVENC on NVIDIA GPUs, Quick Sync on Intel GPUs, AMF on AMD GPUs or else the hardware encoder of Media
Foundation. On Linux and Windows the encoders are tried by encoding a frame with the ffmpeg executable the
`ScreenRecorder` runs, `libx265` is kept when none of them works. Set `hdr` and `fullChroma` before calling
`hardwareAcceleration()`, the encoders are tried with the frames they will encode. The hardware encoders encode with
a constant `quality` from 1 to 100, the default of 55 is comparable to the default quality of `libx265`. Only NVENC
encodes `fullChroma`, the other hardware encoders fall back to `libx265` for it. HDR video is encoded with 10 bits
per component by all encoders.

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            h265 {
                hardwareAcceleration()
                quality = 60
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

`ProresProfile.hardwareAcceleration()` encodes with VideoToolbox on macOS, FFmpeg has no hardware ProRes encoder on
other platforms.
