# orx-osc

Orx-osc is a wrapper around javaOSC

## Usage

```kotlin
// PORT IN and OUT: 57110
val osc = OSC()

osc.listen("/live/track2") {
    // do something
}

osc.send("/maxmsp/filter", 500, "hz")
```
