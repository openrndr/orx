# orx-delegate magic

Collection of magical property delegators. For tracking variable change or
interpolate towards the value of a variable.

## Delegated properties

[Kotlin documentation](https://kotlinlang.org/docs/delegated-properties.html)

## Property smoothing

```kotlin
val state = object {
    var radius = 10.0
}

val smoothRadius by smoothing(state::radius)
```


## Property dynamics

```kotlin
val state = object {
    var radius = 10.0
}

val dynamicRadius by springForcing(state::radius)
```

## Property tracking

```kotlin
val state = object {
    var radius = 10.0
}

val radiusHistory by tracking(state::radius)
```
<!-- __demos__ -->
## Demos
### DemoDifferencing01

Demonstrates the use of the `differencing`, `tracking` and `aggregating` delegates.

All three are used to track changes to a variable. Try changing the `radius` parameter
in the GUI.

The `difference` between the previous value and the current one will be displayed as a red line
that starts in the center of the screen and grows right when the value increases, or left
when the value decreases. As soon as `radius` stops changing, the line becomes invisible
due to having a length of zero.

`tracking` is used to keep track of recent values. The number of samples kept is passed
in its constructor. `tracking` can't be used directly, but it can be passed to an `aggregating`
delegate. In this case it is used to find the maximum value among the last 50 samples
and rendered as a blue line.

Note that new values keep being added to `differenceHistory` and old values discarded.
Therefore, a large increase or decrease in `radius` followed lack of change will be visualized
as a blue line for a short while, until the large value gets replaced by newer values.
How long the large value is visible depends on the `length` parameter passed to `tracking`.

![DemoDifferencing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoDifferencing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDifferencing01.kt)

### DemoFollowing01

Demonstrates using delegate-magic tools with
[Double] and [Vector2].

The white circle's position uses [following].
The red circle's position uses [smoothing].

`following` uses physics (velocity and acceleration).
`smoothing` eases values towards the target.

Variables using delegates (`by`) interpolate
toward target values, shown as gray lines.

The behavior of the delegate-magic functions can be configured
via arguments that affect their output.

The arguments come in pairs of similar name:
The first one, often of type [Double], is constant,
The second one contains `Property` in its name and can be
modified after its creation and even be linked to a UI
to modify the behavior of the delegate function in real time.
The `Property` argument overrides the other.

Notice how the discontinuities present while using `smoothing`
are not there when using `following`.

![DemoFollowing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoFollowing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFollowing01.kt)

### DemoSmoothing01

Demonstrates the use of the `smoothing` delegate, which interpolates
properties over time towards a target value.

In this program, the state of the object is kept in an `object` with
three properties: `x`, `y` and `radius`.

A second set of variables is used to track and smooth changes to
the `state` object: `sx`, `sy` and `sradius`. The `smoothing` factor
is not provided in the constructor, assuming its default value.

The properties in the `state` object are randomly (and independently)
updated with a 1% probability.

By the nature of the used interpolation, changed properties interpolate
first faster and then at a decreasing rate (decelerating) until
reaching the target value.

![DemoSmoothing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSmoothing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoSmoothing01.kt)

### DemoSpring01

Demonstrates the use of `springForcing` to animate the `x`, `y` and `radius`
properties of a circle simulating spring physics.

The target values of all three properties change randomly with a 1% chance.
Note how the spring stiffness is higher for the `x` value.

Since `springForcing` is a method of `Clock`, there is no need to call any
update methods for the values to be interpolated over time.

![DemoSpring01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSpring01Kt.png)

[source code](src/jvmDemo/kotlin/DemoSpring01.kt)
