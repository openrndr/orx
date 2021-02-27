# orx-video-profiles

A collection of `VideoWriterProfile` implementations that can be used with `ScreenRecorder` and `VideoWriter`

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
            profile = GIFProfile()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

Later use `gifsicle` or similar to further reduce file size. For example:

```
$ gifsicle --loop --delay=4 --colors 16 --optimize=2 heavy.gif >lessheavy.gif
```

More about [gifsicle](http://www.lcdf.org/gifsicle/).

### PNG sequence

This profile requires specifying a file name: `outputFile = "frame-%05d.png"`,
where `%05d` means "zero-padded five-digit frame number".
The frame number format is not optional.

```
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            profile = PNGProfile()
            outputFile = "frame-%05d.png"
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

### Animated Webp

```
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            profile = WebpProfile()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```


### Prores (large file, high quality video)

```
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            // .apply is optional, for further configuration
            profile = ProresProfile().apply {
                profile = ProresProfile.Profile.HQ4444
                codec = "prores_ks"
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```

### X265

```
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.*
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            // .apply is optional, for further configuration
            profile = X265Profile().apply {
                mode(X265Profile.WriterMode.Lossless)
                constantRateFactor(23)
                hlg = true
            }
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}
```