# orx-midi

A minimal and limited library for Midi controllers. Orx-midi is a wrapper around javax.midi.

## usage

```kotlin

// -- list all midi devices
MidiDeviceDescription.list().forEach {
    println("${it.name}, ${it.vendor} r:${it.receive} t:${it.transmit}")
}

// -- open a midi controller and listen for control changes
val dev = MidiTransceiver.fromDeviceVendor("BCR2000 [hw:2,0,0]", "ALSA (http://www.alsa-project.org)")
dev.controlChanged.listen {
    println("${it.channel} ${it.control} ${it.value}")
}
```