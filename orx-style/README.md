# orx-style

What style systems share: CSS syntax and values, CSS colors, timing functions and transitions, and registries of properties.

Used by `orx-svg` for the styles of SVG documents and by `orx-panel` for the styles of user interfaces.

## CSS syntax

`parseCssRules` reads the rules of a style sheet, `parseCssDeclarations` the declarations of a block or a style
attribute, with `!important`. `splitCss`, `splitCssList` and `splitCssWords` split values at separators outside of
parentheses and quotes, `evaluateCssCalc` evaluates `calc()`, `parseCssDimension` and `parseCssPixels` read numbers
with units, and `CssKeyword` is `inherit`, `initial` or `unset`.

`CssProperties` is a registry of the properties of a style system, `CssProperty`: their names, whether they are
inherited, their initial values and how values are parsed from text, and their shorthands. `values` gives the values
of a declaration.

```kotlin
object Properties : CssProperties()
val width = Properties.register(CssProperty("width", false, 0.0) { parseCssPixels(it) })
val values = Properties.values(CssDeclaration("width", "2in"))  // [("width", 192.0)]
```

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
