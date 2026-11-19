# orx-expression-evaluator-typed

Evaluate strings containing *typed* mathematical expressions.

# Expression evaluator

Supported types:
 * `Double`
 * `String`
 * `Vector2`
 * `Vector3`
 * `Vector4`
 * `ColorRGBa`

An expression can also be a small program without side effects: statements, separated by semicolons or new lines,
that declare values with `val` for the statements after them, and end in the expression that gives the result.

```kotlin
evaluateTypedExpression("val v = vec2(1.0, 2.0); val s = 3; v * s") // Vector2(3.0, 6.0)
```
