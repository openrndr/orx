# orx-chataigne

With `orx-chataigne` you can share [Chataigne](http://benjamin.kuperberg.fr/chataigne/en) variables within a OPENRNDR project. 

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

