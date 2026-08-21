# orx-osc-query

A minimal [OSCQuery](https://github.com/Vidvox/OSCQueryProposal) server for controlling an
OPENRNDR program from external applications.

OSCQuery combines two transports:

- an **HTTP** server that publishes the OSC namespace as JSON, so clients can *discover* which
  paths exist, their types, ranges and current values;
- an **OSC** (UDP) server that receives value updates and applies them to your program.

It reuses the same annotated `settings` objects as `orx-gui` and `orx-midi` (see `orx-parameters`),
so a single object can be controlled through a GUI, a MIDI controller and OSCQuery at once.

This first version covers what is needed to control a sketch:

| Annotation        | OSC type | Notes                          |
|-------------------|----------|--------------------------------|
| `@DoubleParameter`| `f`      | float, with `MIN`/`MAX` range  |
| `@IntParameter`   | `i`      | integer, with `MIN`/`MAX` range|
| `@ColorParameter` | `r`      | RGBA color                     |
| `@ActionParameter`| `N`      | impulse / trigger              |

## Usage

```kotlin
val settings = @Description("Settings") object {
    @DoubleParameter("radius", 0.0, 100.0)
    var radius = 50.0

    @ColorParameter("fill")
    var color = ColorRGBa.WHITE

    @ActionParameter("randomize")
    fun randomize() { radius = Math.random() * 100.0 }
}

val oscQuery = OSCQuery()   // HTTP + OSC on UDP/TCP port 9000 by default
oscQuery.add(settings)      // exposes /Settings/radius, /Settings/fill, /Settings/randomize
```

The object's `@Description` title (or its class name) becomes a container, and every annotated
property or function becomes an addressable node underneath it.

## Discovering the namespace

With the program running, an OSCQuery client — or a plain browser — can query the HTTP server:

- `http://<host>:9000/` — the full namespace as JSON
- `http://<host>:9000/?HOST_INFO` — where to send OSC (`OSC_IP`, `OSC_PORT`, `OSC_TRANSPORT`)
- `http://<host>:9000/Settings/radius?VALUE` — a single node's current value

Values reported over HTTP always reflect the live property, so changes made through the GUI or a
MIDI controller are visible to OSCQuery clients too.

## Sending updates

Send OSC messages over UDP to the same port using the node's full path as the address:

```
/Settings/radius   77.5      # float
/Settings/sides    9         # int
/Settings/fill     1.0 0.0 0.0 1.0   # RGBA (also accepts an OSC color or packed int)
/Settings/randomize          # impulse triggers the action
```

See `src/demo/kotlin/DemoOSCQuery01.kt` for a complete example.
