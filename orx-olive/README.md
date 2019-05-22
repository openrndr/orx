# orx-olive

Live coding extension for OPENRNDR

## usage

make sure that you add the following to your list of dependencies (next to orx-olive)
```
compile "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.3.31"
```

Then a simple live setup can created as follows:

```kotlin
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.extra.olive.Olive

fun main() = application {
    configure {
        width = 768
        height = 576
    }
    program {
        extend(Olive<Program>())
    }
}
```

The extension will create a template script for you in `src/main/kotlin/live.kts`. You can
edit this to see how the program updates automatically.

## Persistent Data

Sometimes you want to keep parts of your application persistent. In the following example
we show how you can prepare the host program to contain a persistent camera device.

```kotlin
import org.openrndr.Program
import org.openrndr.application

class PersistentProgram: Program() {
    lateinit var camera: FFMPEGVideoPlayer
}

fun main() = application{
    program(PersistentProgram()) {
        camera = FFMPEGVideoPlayer.fromDevice()
        camera.start()

        extend(Olive<PersistentProgram>()) {
            script = "src/main/PersistentCamera.Kt"
        }
    }
}
```

The live script `src/main/PersistentCamera.kt` then looks like this:

```kotlin
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

{ program: PersistentProgram ->
    program.apply {
        extend {
            camera.next()
            drawer.drawStyle.colorMatrix = tint(ColorRGBa.GREEN) * grayscale(0.0, 0.0, 1.0)
            camera.draw(drawer)
        }
    }
}
```
