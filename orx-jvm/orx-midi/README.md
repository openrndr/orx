# orx-midi

MIDI support for keyboards and controllers. Send and receive note and control change events.
Bind inputs to variables.

Orx-midi is a wrapper around javax.midi.

## Usage

```kotlin

// -- list all midi devices
MidiDeviceDescription.list().forEach {
    println("${it.name}, ${it.vendor} r:${it.receive} t:${it.transmit}")
}

// -- open a midi controller and listen for control changes
val dev = MidiTransceiver.fromDeviceVendor(this, "BCR2000 [hw:2,0,0]", "ALSA (http://www.alsa-project.org)")
dev.controlChanged.listen {
    println("${it.channel} ${it.control} ${it.value}")
}

// or program changes
dev.programChange.listen {
    println("${it.channel} ${it.program}")
}
```

## Further reading

The OPENRNDR guide has a [section on orx-midi](https://guide.openrndr.org/#/10_OPENRNDR_Extras/C04_Midi_controllers) that provides step-by-step documentation for using orx-midi in combination with OPENRNDR.
