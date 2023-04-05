# orx-rabbit-control

Creates a web-based remote UI to control your OPENRNDR program from a mobile device or a different computer. Alternative to `orx-gui`. 

`orx-rabbit-control` uses `orx-parameters` annotations to generate a control interface, just like `orx-gui`.
The main difference is that with `orx-gui` the UI is shown on a layer above your program while `orx-rabbit-control` 
shows it on a web browser (potentially on another device). Ideal for tweaking parameters on interactive installations 
without needing to attach a mouse or keyboard to the rendering computer. It also avoids difficulties caused by 
UIs showed on rotated displays or projections. 

<a href="http://rabbitcontrol.cc">
  <img src="http://rabbitcontrol.cc/carrot-sketch-c-trans.png" width="50"> 
</a>

Find examples under the [demo](./src/demo/kotlin) folder.

### Accessing the generated web UI

Once you start a program that uses orx-rabbit-control, a QR code will be displayed on a layer above your visuals
until someone accesses the web UI. 

The UI can be accessed in a web browser in three different ways:

- scan the QR code with a mobile device connected to the same wireless network,
- or click on the URL displayed in the IDE console,
- or go to [client.rabbitcontrol.cc](http://client.rabbitcontrol.cc) and enter your IP-address and port (displayed at the end of the URL shown in the IDE console)

Once the UI is visible in a web browser one can interact with the sliders, buttons, checkboxes etc. 
to control the OPENRNDR program remotely.

More info about the web client: 
[rabbitcontrol.cc/apps/webclient/](http://rabbitcontrol.cc/apps/webclient/)

### Screenshot of a simple web UI

<img src="https://rabbitcontrol.cc/apps/webclient/webclient.png" width="150">

### Frequently asked questions

[https://rabbitcontrol.cc/faq/](https://rabbitcontrol.cc/faq/)
<!-- __demos__ -->
## Demos
### DemoRabbitControl
[source code](src/demo/kotlin/DemoRabbitControl.kt)

![DemoRabbitControlKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-rabbit-control/images/DemoRabbitControlKt.png)

### DemoRabbitControlManualOverlay
[source code](src/demo/kotlin/DemoRabbitControlManualOverlay.kt)

![DemoRabbitControlManualOverlayKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-rabbit-control/images/DemoRabbitControlManualOverlayKt.png)

### DemoRabbitHole
[source code](src/demo/kotlin/DemoRabbitHole.kt)

![DemoRabbitHoleKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-rabbit-control/images/DemoRabbitHoleKt.png)
