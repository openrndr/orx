# orx-chataigne

Expose variables to [Chataigne](http://benjamin.kuperberg.fr/chataigne/en) and any other applications that can interface with it.
The current implementation makes use of the OSC protocol and supports `Double` and `ColorRGBa`.

## Usage

Defining the variables
```kotlin
class SceneVariables : ChataigneOSC(OSC(portIn = 9005, portOut = 12001)) {
        val myRadius: Double by DoubleChannel("/myRadius")
        val myOpacity: Double by DoubleChannel("/myOpacity")
        val myColor: ColorRGBa by ColorChannel("/myColor")
}
```

Initiate

```kotlin
 val animation = SceneVariables()
```

Update time

```kotlin
animation.update(seconds)
```

Use the variables

```kotlin
animation.myRadius
animation.myOpacity
animation.myColor
```

## Example project

Find the Chataigne example project in `/resources/timeline_example_chataigne.noisette` which works together with demo project `/src/demo/kotlin/ChataigneOSCDemo.kt`

<!-- __demos__ -->
## Demos
### ChataigneOSCDemo


![ChataigneOSCDemoKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-chataigne/images/ChataigneOSCDemoKt.png)

[source code](src/demo/kotlin/ChataigneOSCDemo.kt)
