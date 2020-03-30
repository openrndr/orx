# orx-parameters

A collection of annotations and tools that are used to turn Kotlin properties into introspectable parameters. Parameters 
are highly suitable for automatically generating user interfaces, but note that this is _not_ what `orx-parameters` does.

For an example (and a highly usable implementation) of generating interfaces from the annotations you are encouraged to check out [`orx-gui`](../orx-gui/README.md). 

Currently orx-parameters facilitates the following annotations:

 - `DoubleParameter` for `Double` properties
 - `IntParameter` for `Int` properties
 - `BooleanParameter` for `Boolean` properties
 - `TextParameter` for `String` properties
 - `ColorParameter` for `ColorRGBa` properties
 - `XYParameter` for `Vector2` properties
 - `Vector2Parameter`  for `Vector2` properties
 - `Vector3Parameter` for `Vector3` properties
 - `Vector4Parameter` for `Vector4` properties

Additionally there is an `ActionParameter` that can be used to annotate functions without arguments.

## Annotation application

Annotations can be applied to a properties inside a class or object class.

````kotlin
val foo = object {
    @DoubleParameter("a double scalar", 0.0, 1.0, order = 0)
    var d = 1.0

    @IntParameter("an integer scalar", 1, 100, order = 1)
    var i = 1

    @BooleanParameter("a boolean parameter", order = 2)
    var b = false
    
    @XYParameter("an XY parameter", order = 3)
    var xy = Vector2.ZERO

    @XYParameter("a Vector2 parameter", order = 4)
    var v2 = Vector2.ZERO

    @XYParameter("a Vector3 parameter", order = 5)
    var v3 = Vector3.ZERO

    @XYParameter("a Vector4 parameter", order = 6)
    var v4 = Vector4.ZERO

    @ActionParameter("a simple action", order = 7)
    fun actionFunction() {
        // -- 
    }
}
````

## Querying parameters

Given an instance of an annotated class we can list the parameters using the extension method 
`Any.listParameters()` of our previously declared object `foo` 

```kotlin
    import org.openrndr.extra.parameters.listParameters

    // ..

    val parameters = foo.listParameters()
```

