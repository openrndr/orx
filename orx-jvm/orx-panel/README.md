# orx-panel

The OPENRNDR UI toolkit. Provides buttons, sliders, text, a color picker and much more. HTML/CSS-like.

<!-- __demos__ -->
## Demos
### DemoBinding01

Demonstrates how to create a UI with two sliders and a button, and how to bind
the value of the sliders with properties of an object's instance.

`styleSheet` is used to control the looks and placement of the inputs
(equivalent to CSS in web pages) and `layout` represents the content of the UI,
including names, ID and slider ranges (equivalent to HTML in web pages).

![DemoBinding01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoBinding01Kt.png)

[source code](src/demo/kotlin/DemoBinding01.kt)

### DemoColorPickerButton01

A simple demonstration of a ColorPickerButton

![DemoColorPickerButton01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoColorPickerButton01Kt.png)

[source code](src/demo/kotlin/DemoColorPickerButton01.kt)

### DemoComplex01

Demonstrates how to create a UI with a drop-down menu. When an option is picked,
the content of a Div is replaced by a button and some sliders.

![DemoComplex01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoComplex01Kt.png)

[source code](src/demo/kotlin/DemoComplex01.kt)

### DemoGridLayout01

Demonstrates the use of grid layouts.

The program creates a grid of 2 columns and 4 rows. The first two rows are merged together by using
`gridPopulation { 2.columns }`.

![DemoGridLayout01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoGridLayout01Kt.png)

[source code](src/demo/kotlin/DemoGridLayout01.kt)

### DemoHorizontalLayout01

Demonstrates how to create a `styleSheet` using `Display.FLEX` and `FlexDirection.Row`
to create a horizontal layout featuring 10 clickable buttons with various colors.

The `controlManager { }` DSL includes `styleSheet { }`, which uses a syntax inspired
by CSS, and `layout { }`, which is structured similarly to HTML.

![DemoHorizontalLayout01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoHorizontalLayout01Kt.png)

[source code](src/demo/kotlin/DemoHorizontalLayout01.kt)

### DemoSimpleUI01

Demonstrates the use of grid layouts, property-control binding, and JSON serialization/deserialization for
model persistence.

![DemoSimpleUI01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoSimpleUI01Kt.png)

[source code](src/demo/kotlin/DemoSimpleUI01.kt)

### DemoToolWindows01

Demonstrates how to create a simple UI with a button to open a secondary tool window with multiple sliders.

A `hitTest` area at the top of the tool window makes it possible to drag it with the mouse.

The tool window can be closed by clicking its `close` button, or by pressing the ESC key.

![DemoToolWindows01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoToolWindows01Kt.png)

[source code](src/demo/kotlin/DemoToolWindows01.kt)

### DemoVerticalLayout01

Demonstrates how `Display.FLEX` can be used to create vertical arrangements by
using `FlexDirection.Column` instead of `FlexDirection.Row`.

The program also shows common `styleSheet` properties to control `width`, `height`,
padding and background color.

![DemoVerticalLayout01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoVerticalLayout01Kt.png)

[source code](src/demo/kotlin/DemoVerticalLayout01.kt)

### DemoViewBoxElement01

This demonstration shows how `ViewBox` (from `orx-view-box`) can be embedded as a document element within the panel layout system,
allowing OPENRNDR drawing operations to be integrated alongside other UI components in a grid layout.
The ViewBox element responds interactively to UI controls (slider) and mouse input.

![DemoViewBoxElement01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoViewBoxElement01Kt.png)

[source code](src/demo/kotlin/DemoViewBoxElement01.kt)

### DemoViewBoxElement02

This demonstration shows how a previously defined `ViewBox` (from `orx-view-box`) can be embedded as a document element within the panel layout system,
allowing OPENRNDR drawing operations to be integrated alongside other UI components in a grid layout.
The ViewBox element responds interactively to UI controls (slider) and mouse input.

![DemoViewBoxElement02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoViewBoxElement02Kt.png)

[source code](src/demo/kotlin/DemoViewBoxElement02.kt)

### DemoWatchDiv01

Demonstrates how to create a GUI with a persistent state (can be saved and loaded)
and a variable number of inputs arranged in a grid. The user can adjust
the number of columns and rows of the grid, and each cell features a slider
to control its value between 0.0 and 10.0 (the default range).

![DemoWatchDiv01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoWatchDiv01Kt.png)

[source code](src/demo/kotlin/DemoWatchDiv01.kt)

### DemoWatchObjectDiv01

A demonstration of `watchObjectDiv`.

`watchObjecDiv` creates a Div element with the provided `classes`, and runs a `builder` function
to populate the Div any time the `watchObject` argument changes.

This demo creates a panel with two sliders bound to two integer values, representing the number of columns
and rows.

The `watchObjectDiv` expects three arguments: the classes to apply to the Div, an object to watch, and a
builder function to populate the Div object when the watched object changes.

The builder function creates a Div for every row, containing a Button for every column.
The label for each button is its coordinates. A `clicked` event is attached to the buttons to print
those coordinates.


![DemoWatchObjectDiv01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-panel/images/DemoWatchObjectDiv01Kt.png)

[source code](src/demo/kotlin/DemoWatchObjectDiv01.kt)
