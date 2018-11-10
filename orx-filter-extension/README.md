# orx-filter-extension

Provides extension methods for Program that can be used to provide Filters to the `extend()` mechanism

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