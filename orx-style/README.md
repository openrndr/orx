# orx-style

What style systems share: CSS colors, CSS timing functions and transitions.

Used by `orx-svg` for the styles of SVG documents and by `orx-panel` for the styles of user interfaces.

## Colors

`parseCssColor` parses CSS colors: named colors, hexadecimal colors and color functions, among them `rgb()`,
`hsl()`, `hwb()`, `lab()`, `lch()`, `oklab()`, `oklch()`, `color()` and `color-mix()`.

```kotlin
val color = parseCssColor("oklch(70% 0.1 200)")
```

## Transitions

`TimingFunction` is a timing function of CSS: `TimingFunction.css("ease-in-out")`,
`TimingFunction.cubicBezier(0.4, 0.0, 0.2, 1.0)` or `TimingFunction.steps(4, "jump-end")`. `TransitionTiming` is the
duration, delay and timing function of a transition, `TransitionTiming.css` reads them from the `transition`
declarations of CSS.

`Transitions` runs the transitions of the properties of one element, for values of any type with the interpolation
that is given: a transition starts from the value that is shown, also when another transition of the property runs.
