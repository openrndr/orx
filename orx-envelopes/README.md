# orx-envelopes

ADSR (Attack, Decay, Sustain, Release) envelopes and tools.

## ADSR

Attack, decay, sustain, release.

<!-- __demos__ -->
## Demos
### DemoADSRTracker01

Demonstrates how to use an `ADSRTracker`.

The `ADSRTracker` keeps a collection of active ADSR envelopes.

An ADSR envelope is commonly used in musical synthesizers to track the
volume of a sound produced in response to pressing, holding and releasing
a key in it keyboard.

The ADSR envelope in OPENRNDR, when `triggerOn` is called, tracks the change of a
Double value increasing from 0.0 to 1.0 in `attack` seconds,
then decays to the `sustain` level in `decay` seconds, and finally when
`triggerOff` is called, decreases back to 0.0 in `release` seconds.

The time in seconds is tracked by a `Clock` passed in the constructor.
This allows to use alternative clocks, for instance, frame-based.

In this interactive program the `t` key can be pressed, held, and released to
go through the ADSR cycle. Try also pressing the `t` key repeatedly and observe
how multiple instances are tracked.

The current ADSR instances are visualized as growing and shrinking circles at the
top of the window.

In the center of the window one can see the added value of all current ADSR instances
represented as the radius of a white circle.


![DemoADSRTracker01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-envelopes/images/DemoADSRTracker01Kt.png)

[source code](src/jvmDemo/kotlin/DemoADSRTracker01.kt)

### DemoADSRTracker02

Demonstrates using `ADSRTracker`. The core difference
with `DemoADSRTracker01` is how shapes are rendered.

Both programs listen to key presses, but the first
program renders tracked shapes inside the `extend` block,
while this program attaches a unique rendering block
to each tracked shape.

The `ADSRTracker` maintains a mutable list of trackers,
but they do not have a stable ID. The element with index 3
will have index 2 when elements with lower indices expire.
This is the reason why visualized elements jump left when
an older tracker runs through its complete cycle.

Attaching a function to each `triggerOn` event allows
rendered shapes to have a stable position on the window.

Notice how the program works with two different `triggerId`s:
one used when pressing the `t` key, and the other for the `r`
key.

This is needed on the `triggerOff` calls: to tell the tracker
which type of element should wind down. If several items
with that same `triggerId` exist (when we repeatedly pressed
the same keyboard key), the most recent of them will receive
the event.

![DemoADSRTracker02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-envelopes/images/DemoADSRTracker02Kt.png)

[source code](src/jvmDemo/kotlin/DemoADSRTracker02.kt)
