# orx-delegate magic

Collection of magical property delegators

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
