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

<!-- __demos__ -->
## Demos
### DemoRepeat01

A simple demonstration on using the `repeat` method to execute a function
at regular intervals.

Note that drawing inside the repeat action has no effect.
See DemoRepeat02.kt to learn how to trigger drawing.


![DemoRepeat01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoRepeat01Kt.png)

[source code](src/demo/kotlin/DemoRepeat01.kt)

### DemoRepeat02

This demonstrates how to combine `repeat {}` with a postponed event to trigger drawing.

![DemoRepeat02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoRepeat02Kt.png)

[source code](src/demo/kotlin/DemoRepeat02.kt)

### DemoRepeat03

Shows how a `repeat` block can update a variable used
for rendering. In this demo, the `opacity` variable is
reduced on every animation frame, and increased to 1.0
every 2 seconds, creating a pulsating animation effect.

![DemoRepeat03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoRepeat03Kt.png)

[source code](src/demo/kotlin/DemoRepeat03.kt)

### DemoTimeOut01

Demonstrates the `timeOut` function.

It is similar to the `repeat` function,
but it runs only once after the specified delay in seconds.


![DemoTimeOut01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-timer/images/DemoTimeOut01Kt.png)

[source code](src/demo/kotlin/DemoTimeOut01.kt)
