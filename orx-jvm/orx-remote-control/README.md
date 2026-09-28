# orx-remote-control

Add remote control functionality to Programs via HTTP API.

## Usage

`RemoteControl` exposes a running `Program` over a local HTTP server: request a screenshot of
the current frame, inject keyboard/mouse events as if a real user produced them, or freeze the
animation clock at a fixed time for reproducible captures. It's meant for driving an OPENRNDR
program from an external tool (a test suite, an agent, a browser) that can't attach to the
window directly.

```kotlin
import org.openrndr.application
import org.openrndr.extra.remotecontrol.RemoteControl

fun main() = application {
    program {
        extend(RemoteControl()) {
            port = 9000          // default
            network = "127.0.0.1" // default -- localhost-only unless changed deliberately
        }
        extend {
            // ... your program
        }
    }
}
```

All routes are served on `network:port` (default `127.0.0.1:9000`, i.e. not reachable from
outside the machine by default). All keyboard/mouse/clock actions are queued and applied on the
program's own thread right before the next frame is drawn, so it's safe to call these routes
from any thread.

## Routes

### `GET /screenshot`

Waits for the next frame and returns it as a PNG.

```bash
curl -s -o screenshot.png http://127.0.0.1:9000/screenshot
```

### `POST /keyboard/{down,up,repeat}`

Injects a key event. `name` is the layout-sensitive key name as used by
`org.openrndr.KeyEvent.name` (e.g. `a`, `spacebar`, `arrow-left`). An optional `modifiers`
query parameter takes a comma-separated list of `SHIFT`, `CTRL`, `ALT`, `SUPER`.

```bash
curl -s -X POST "http://127.0.0.1:9000/keyboard/down?name=spacebar"
curl -s -X POST "http://127.0.0.1:9000/keyboard/up?name=spacebar"
curl -s -X POST "http://127.0.0.1:9000/keyboard/repeat?name=a"
curl -s -X POST "http://127.0.0.1:9000/keyboard/down?name=a&modifiers=SHIFT,CTRL"
```

### `POST /mouse/move`

Moves the mouse to an absolute window position, given by `x`/`y`.

```bash
curl -s -X POST "http://127.0.0.1:9000/mouse/move?x=200&y=150"
```

### `POST /mouse/button/{down,up}`

Presses/releases a mouse button (`LEFT`, `RIGHT` or `CENTER`, default `LEFT`), optionally
moving to `x`/`y` first (omit them to use the current position). Also takes an optional
`modifiers` query parameter, same as `/keyboard`.

```bash
curl -s -X POST "http://127.0.0.1:9000/mouse/button/down?button=LEFT&x=360&y=360"
curl -s -X POST "http://127.0.0.1:9000/mouse/button/up?button=LEFT&x=600&y=360"
curl -s -X POST "http://127.0.0.1:9000/mouse/button/down?button=RIGHT"
```

### `POST /mouse/scroll`

Injects a scroll event with the given `dx`/`dy` (each defaults to `0.0`).

```bash
curl -s -X POST "http://127.0.0.1:9000/mouse/scroll?dx=0&dy=-1"
```

### `POST /clock` and `POST /clock/reset`

Freezes `Program.seconds` at a fixed `time` (in seconds), e.g. for deterministic, reproducible
screenshots of an animation at a specific point in time. `/clock/reset` restores the clock that
was active before `/clock` first overrode it.

```bash
curl -s -X POST "http://127.0.0.1:9000/clock?time=1.5707963"
curl -s -X POST "http://127.0.0.1:9000/clock/reset"
```
<!-- __demos__ -->
## Demos
### DemoRemoteControl01

A small, visibly-reactive program to drive with [RemoteControl]: draws a circle that
follows the (possibly emulated) mouse position, flips the background color on spacebar,
and orbits a dot based on [org.openrndr.Program.seconds] -- so freezing it with `/clock`
visibly stops the dot instead of just leaving a static scene unprovable either way. Useful
for checking, end to end, that HTTP requests to `/screenshot`, `/mouse/move`,
`/keyboard/down` and `/clock` actually reach the running program.

![DemoRemoteControl01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-remote-control/images/DemoRemoteControl01Kt.webp)

[source code](src/demo/kotlin/DemoRemoteControl01.kt)
