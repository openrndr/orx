# orx-parameters

Annotations for turning Kotlin properties into introspectable parameters.
Used by [`orx-gui`](../orx-jvm/orx-gui/README.md) to generate user interfaces, and  by [`orx-midi`](../orx-jvm/orx-midi/README.md) to bind hardware inputs to variables.

Note that `orx-parameters` does _not_ generate user interfaces; 
it only provides the information needed to create them.

For an example (and a highly usable implementation) of generating interfaces 
from the annotations you are encouraged to check out [`orx-gui`](../orx-jvm/orx-gui/README.md). 

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
 - `DoubleListParameter` for `List<Double>` properties
 - `OptionParameter` for `Enum` properties

Additionally there is an `ActionParameter` that can be used to annotate functions without arguments.

## Annotation application

Annotations can be applied to properties inside a class or object class.

```kotlin
val foo = object {
    @DoubleParameter("a double scalar", 0.0, 1.0, order = 0)
    var d = 1.0

    @IntParameter("an integer scalar", 1, 100, order = 1)
    var i = 1

    @BooleanParameter("a boolean parameter", order = 2)
    var b = false
    
    @XYParameter("an XY parameter", order = 3)
    var xy = Vector2.ZERO

    @Vector2Parameter("a Vector2 parameter", order = 4)
    var v2 = Vector2.ZERO

    @Vector3Parameter("a Vector3 parameter", order = 5)
    var v3 = Vector3.ZERO

    @Vector4Parameter("a Vector4 parameter", order = 6)
    var v4 = Vector4.ZERO

    @ActionParameter("a simple action", order = 7)
    fun actionFunction() {
        // -- 
    }
    @DoubleListParameter("a double-list parameter", order = 8)
    var dl = mutableListOf(0.0, 0.0, 0.0)

    @OptionParameter("an option parameter", order = 9)
    var option = SomeEnum.SomeValue
}
```

## Querying parameters

Given an instance of an annotated class we can list the parameters using the extension method 
`Any.listParameters()` of our previously declared object `foo` 

```kotlin
    import org.openrndr.extra.parameters.listParameters

    // ..

    val parameters = foo.listParameters()
```


## Binding parameters

`ParameterBindings` (JVM) binds the parameters of objects by labels, without a GUI. Values are read, set,
randomized, saved and loaded by the label of an object and the name of a parameter. Objects held by properties with
a `@NestedParameter` annotation are bound too, labeled `<label>/<label of the nested parameter>`.

```kotlin
val bindings = ParameterBindings()
bindings.add(foo, "foo")

bindings["foo", "d"] = 0.5          // set a value by the name of its property
val d = bindings["foo", "d"]        // read it
bindings.invoke("foo", "someAction") // call an action
bindings.randomize(0.1)             // move values towards random values in their ranges

bindings.changed.listen { println("${it.binding.label}.${it.parameter.name} = ${it.value}") }

bindings.save(File("parameters.json"))
bindings.load(File("parameters.json"))
```

The files are those that the GUI of orx-gui saves and loads. The GUI uses `ParameterBindings` too: pass one to
`GUI(bindings = ...)` to share it with other code, like a remote control or a MIDI controller; the GUI shows the
objects that are added to it and reports the changes of its controls through `changed`.

The values of `@ListParameter` and `@MutableListParameter` properties implement `Selectable`, like the
`SelectableList` and `SelectableMutableList` of orx-panel.
