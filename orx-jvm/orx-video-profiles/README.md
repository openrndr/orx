# orx-video-profiles

GIF, H265, PNG, Prores, TIFF and Webp `VideoWriterProfile`s for `ScreenRecorder` and `VideoWriter`.

## Usage

### GIF

```
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

```
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
                constantRateFactor(23)
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```
