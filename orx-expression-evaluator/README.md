# orx-expression-evaluator

Tools to evaluate strings containing mathematical expressions.

# Expression evaluator

```kotlin
val expression = "x + y"
val constants = mapOf("x" to 1.0, "y" to 2.0)
evaluateExpression(expression, constants)
```
## Built-in expression functions

Unary functions:
 * `abs(x)`
 * `acos(x)`
 * `asin(x)`
 * `atan(x)`
 * `ceil(x)`
 * `cos(x)`
 * `degrees(x)`
 * `exps(x)`
 * `floor(x)`
 * `radians(x)`
 * `round(x)`
 * `saturate(x)`, clamp x to [0.0, 1.0]
 * `sqrt(x)`
 * `tan(x)`

Binary functions:
 * `atan2(x, y)`
 * `length(x, y)`, the Euclidean length of the vector (x,y)
 * `max(x, y)`,
 * `min(x, y)`,
 * `pow(x, n)`
 * `random(x, y)`, return a random number in [x, y)
 
Ternary functions:
 * `length(x, y, z)`, the Euclidean length of the vector (x, y, z)
 * `max(x, y, z)`
 * `min(x, y, z)`
 * `mix(l, r, f)`
 * `smoothstep(e0, e1, x)`
 * `sum(x, y, z)`

Quaternary functions:
* `length(x, y, z, w)`, the Euclidean length of the vector (x, y, z)
* `max(a, b, c, d)`
* `min(a, b, c, d)`
* `sum(a, b, c, d)`

Quinary functions:
* `map(x0, x1, y0, y1, v)`
* `max(a, b, c, d, e)`
* `min(a, b, c, d, e)`
* `sum(a, b, c, d, e)`

# Compiled functions

```kotlin
val expression = "x * 5.0 + cos(x)"
val f = compileFunction1(expression, "x")
f(0.0)
```

```kotlin
val expression = "x * 5.0 + cos(x) * y"
val f = compileFunction2(expression, "x", "y")
f(0.0, 0.4)
```

# Property delegates

```kotlin
val constants = mutableMapOf("width" to 300.0)
val settings = object {
    var xExpression = "cos(t) * 50.0 + width / 2.0"
}
val xFunction by watchingExpression1(settings::xExpression, "t", constants)

xFunction(1.0)
```
<!-- __demos__ -->
## Demos
### DemoExpressionEvaluator01

Demonstrates the use of `evaluateExpression` to process strings containing mathematical expressions.

In this demo, expressions are evaluated on every animation frame. Notice how the `t`, `width`,
and `height` values are passed to the evaluator, allowing these variables to be used directly
within the expressions.

Evaluating expressions enables dynamic behavior adjustments at runtime, eliminating the need
to recompile the program.

![DemoExpressionEvaluator01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-expression-evaluator/images/DemoExpressionEvaluator01Kt.png)

[source code](src/jvmDemo/kotlin/DemoExpressionEvaluator01.kt)

### DemoExpressionEvaluator02

Improved version of DemoExpressionEvaluator01, it uses [watchingExpression1] to automatically convert an expression
string into a function with a parameter "t".

By using [watchingExpression1], the resulting function is only updated when the content of its first argument
(a String) changes, which uses less CPU than evaluating the expression on every animation frame.

![DemoExpressionEvaluator02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-expression-evaluator/images/DemoExpressionEvaluator02Kt.png)

[source code](src/jvmDemo/kotlin/DemoExpressionEvaluator02.kt)

### DemoExpressionEvaluator03

Spirograph-like demo of orx-expression-evaluator, using [watchingExpression1] to automatically convert an expression
String into a function with a parameter "t".

The program generates a list with 2000 points and draws a line strip connecting them.

Editing the expression may make it temporarily invalid. When this happens, it returns 0.0 (for instance,
due to non-matching parenthesis). This is normally not an issue, but the `lineStrip()` method fails
when consecutive points are identical, which is the case if all points are Vector2(0.0).
Therefore, we wrap it in a try/catch to keep the program running even while the expressions are not valid.

![DemoExpressionEvaluator03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-expression-evaluator/images/DemoExpressionEvaluator03Kt.png)

[source code](src/jvmDemo/kotlin/DemoExpressionEvaluator03.kt)
