# orx-olive

Provides live coding functionality: updates a running OPENRNDR program when you save your changes.

## usage

make sure that you add the following to your list of dependencies (next to orx-olive)
```
implementation "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.3.31"
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

## Shade style errors

Recent versions of `orx-olive` automatically set the `org.openrndr.ignoreShadeStyleErrors` property which
makes OPENRNDR ignore errors in the shade style and return the default shader. To get this behaviour in 
older versions add `-Dorg.openrndr.ignoreShadeStyleErrors=true` to the JVM arguments.

## Reloadable State

Along with the extension comes a mechanism that allows state to be reloaded from a store on script reload.
This functionality is offered by the `Reloadable` class.

An example `live.kts` in which the reloadable state is used:
```kotlin
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

{ program: PersistentProgram ->
    program.apply {
        val a = object : Reloadable() {
            var x : Double = 0.0
        } 
        a.reload()

        extend {
            // do something with a.x here
        }
    }
}
```

The Reloadable store can be cleared using the `clearReloadables` function. 

### Reloadable GPU resources

To store GPU resources or objects that use GPU resources (a.o. `ColorBuffer`, `VertexBuffer`, `Shader`, `BufferTexture`)  in a `Reloadable` object one uses OPENRNDR's 
`persistent {}` builder function.

```!kotlin
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

{ program: PersistentProgram ->
    program.apply {
        val a = object : Reloadable() {
            var image = persistent { loadImage("data/images/pm5544.png" ) }
        }
        a.reload()

        extend {
            drawer.image(a.image)
        }
    }
}
```




Keep in mind that `Reloadable` should only be used for singleton classes.

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

The live script `src/main/PersistentCamera.kts` then looks like this:

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
<!-- __demos__ -->
## Demos
### DemoOlive01
[source code](src/demo/kotlin/DemoOlive01.kt)

![DemoOlive01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-olive/images/DemoOlive01Kt.png)

### DemoOliveScriptless01
[source code](src/demo/kotlin/DemoOliveScriptless01.kt)

![DemoOliveScriptless01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-olive/images/DemoOliveScriptless01Kt.png)
