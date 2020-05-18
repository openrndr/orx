# orx-filter-extension

To apply graphics filters on every animation frame using `extend(FILTER_NAME())`.

##### API

```kotlin
fun <F : Filter> Program.extend(filter: F, configuration: F.() -> Unit = {}): Extension
```

##### Usage

```kotlin
fun main() = application {
    program {
        extend(FXAA()) {
            // this function is executed every frame
        }
    }
}
```
