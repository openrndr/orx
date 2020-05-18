# orx-timer

Simple timer functionality providing `repeat`, to run code with a given interval
and `timeOut`, to run code once after a given delay.

## Prerequisites

Add `orx-timer` to the `orxFeatures` set in your build.gradle.kts

## Usage

`orx-timer` facilitates two extension functions for `Program`

`fun Program.repeat(intervalInSeconds: Double, count: Int? = null, initialDelayInSeconds: Double = 0.0, action: () -> Unit)`

`fun Program.timeOut(delayInSeconds: Double, action: () -> Unit)`

A simple example looks like this:

```kotlin
fun main() = application {
    program {
        repeat(2.0) {
            println("hello there")
        }
        extend {

        }
    }
}
```

Note that drawing inside the `repeat` action has no effect. Have a look at the demos listed below for an example of
`repeat` triggered drawing.

## Demos

 * [Simple `repeat` demonstration](src/demo/kotlin/DemoRepeat01.kt)
 * [A `repeat` demonstration with drawing](src/demo/kotlin/DemoRepeat02.kt)
 * [Simple `timeOut` demonstration](src/demo/kotlin/DemoTimeOut01.kt)
<!-- __demos__ >
# Demos
[DemoRepeat01Kt](src/demo/kotlin/DemoRepeat01Kt.kt
![DemoRepeat01Kt](https://github.com/openrndr/orx/blob/media/orx-timer/images/DemoRepeat01Kt.png
[DemoRepeat02Kt](src/demo/kotlin/DemoRepeat02Kt.kt
![DemoRepeat02Kt](https://github.com/openrndr/orx/blob/media/orx-timer/images/DemoRepeat02Kt.png
[DemoTimeOut01Kt](src/demo/kotlin/DemoTimeOut01Kt.kt
![DemoTimeOut01Kt](https://github.com/openrndr/orx/blob/media/orx-timer/images/DemoTimeOut01Kt.png
<!-- __demos__ -->
## Demos
### DemoRepeat01
[source code](src/demo/kotlin/DemoRepeat01.kt)

![DemoRepeat01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoRepeat01Kt.png)

### DemoRepeat02
[source code](src/demo/kotlin/DemoRepeat02.kt)

![DemoRepeat02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoRepeat02Kt.png)

### DemoTimeOut01
[source code](src/demo/kotlin/DemoTimeOut01.kt)

![DemoTimeOut01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoTimeOut01Kt.png)
