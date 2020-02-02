# orx-parameters

A collection of annotations and tools that are used to turn Kotlin properties into introspectable parameters. Parameters 
are highly suitable for automatically generating user interfaces, but note that this is _not_ what `orx-parameters` does.

Currently orx-parameters supplies the following annotations:

 - `DoubleParameter`
 - `IntParameter`
 - `BooleanParameter`
 - `TextParameter`

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

