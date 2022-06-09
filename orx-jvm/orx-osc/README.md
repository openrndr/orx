# orx-osc

Open Sound Control makes it possible to send and receive messages
from other OSC enabled programs in the same or a different computer.
Used to create multi-application or multi-device software.

Can be used to remote control your program from a mobile device,
to produce sound in a audio programming environment, make games
and networked experiments.

Orx-osc is a wrapper around javaOSC

## Usage

```kotlin
// PORT IN and OUT: 57110
val osc = OSC()

osc.listen("/live/track2") { addr, msg ->
    // do something
}

osc.send("/maxmsp/filter", 500, "hz")
```

For more examples please visit the [guide](https://guide.openrndr.org/OPENRNDRExtras/osc.html).
