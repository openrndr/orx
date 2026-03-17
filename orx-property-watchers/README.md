# orx-property-watchers

Tools for setting up property watcher based pipelines

<!-- __demos__ -->
## Demos
### DemoImagePathWatcher01

Demonstrates `watchingImagePath()` and `watchingProperty()`.

`watchingImagePath()` detects changes to a String, loads the image the String points to, and returns it as a
ColorBuffer. It allows transforming the loaded image, for instance, by making it grayscale, resizing it,
or applying a filter.

`watchingProperty()` detects changes to the watched variable which can be of any type. The returned type is not
fixed and is determined by whatever is returned by its `function` argument. A `cleaner` argument, if present,
will be executed before calling `function`, ideal to free resources.

Press the `ENTER` key to update the `state.path` variable, which will trigger an update of `state.image`, followed
by an update to `state.redImage`.

![DemoImagePathWatcher01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-property-watchers/images/DemoImagePathWatcher01Kt.png)

[source code](src/jvmDemo/kotlin/DemoImagePathWatcher01.kt)

### DemoPropertyWatchers01

This program demonstrates how to use `watchingProperty()` to track changes
to variables.

The method is used to define and keep two variables up to date:
First, it creates a variable `x` that tracks the horizontal position of the mouse.
Second, it creates the variable `xx` with the square of `x` and
keeps it up to date by watching it change.

Note how the variables are defined just once outside the draw loop
but are kept up to date nonetheless.

The two variables are used to control the position and the thickness of a
horizontal black line.

![DemoPropertyWatchers01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-property-watchers/images/DemoPropertyWatchers01Kt.png)

[source code](src/jvmDemo/kotlin/DemoPropertyWatchers01.kt)
