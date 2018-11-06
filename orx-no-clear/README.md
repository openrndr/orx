# orx-no-clear

A simple OPENRNDR Extension that provides the classical drawing-without-clearing-the-screen functionality that
OPENRNDR does not support natively.

#### Usage

```
class NoClearProgram: Program() {

    override fun setup() {
        backgroundColor = ColorRGBa.PINK
        extend(NoClear())
    }

    override fun draw() {
        drawer.circle(Math.cos(seconds) * width / 2.0 + width / 2.0, Math.sin(seconds * 0.24) * height / 2.0 + height / 2.0, 20.0)
    }
}

```
