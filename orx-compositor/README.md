# orx-compositor

Toolkit to make composite (layered) images using blend modes and filters.

## Usage

A `Composite` is made using the `compose {}` builder. We start with a very simple example:
```kotlin
fun main() = application {
    program {
        val composite = compose {
            // this is only executed once
            val position = Vector2(100.0, 100.0)
            
            draw {
                // code inside draw blocks is executed whenever the composite is drawn
                drawer.circle(position, 100.0)
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```

### Layers

A `Composite` with two layers looks like this.

```kotlin
fun main() = application {
    program {
        val composite = compose {
            // this layer is drawn first
            layer {
                val position = Vector2(100.0, 100.0)
                draw {
                    drawer.circle(position, 100.0)
                }
            }
            
            // this layer is drawn second
            layer {
                val position = Vector2(150.0, 150.0)
                draw {
                    drawer.circle(position, 100.0)
                }
                blend(Multiply())
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```

Layers can be nested:

```kotlin
fun main() = application {
    program {
        val composite = compose {
            layer {
                layer {
                    // this draw is processed first
                    draw { }
                }
                layer {
                    // this draw is processed second
                    draw { }
                }
                val position = Vector2(100.0, 100.0)
                draw {
                    // this draw is processed third
                    drawer.circle(position, 100.0)
                }
            }
            
            // this layer is drawn second
            layer {
                val position = Vector2(150.0, 150.0)
                draw {
                    drawer.circle(position, 100.0)
                }
                blend(Multiply())
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```

### Asides

An aside is a layer which output is not directly included in the composite drawing. The contents of an aside can be used in layers and post-processing.

```kotlin
fun main() = application {
    program {
        val composite = compose {
            
            val a = aside {
                val position = Vector2(250.0, 250.0)
                draw {
                    drawer.circle(position, 100.0)
                }
            }
            
            // this layer is drawn second
            layer {
                val position = Vector2(150.0, 150.0)
                draw {
                    drawer.image(a)
                    drawer.circle(position, 100.0)
                }
                blend(Multiply())
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```


### Post-processing

```kotlin
fun main() = application {
    program {
        val composite = compose {
            layer {
                draw {
                    
                }
                // the first Filter1to1 to apply
                post(ApproximateGaussianBlur()) {
                    // here is code that is executed everytime the layer is drawn
                }
                
                // the second Filter1to1 to apply
                post(ColorCorrection()) {
                    // here is code that is executed everytime the layer is drawn
                }
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```
#### Using filters with multiple inputs

Some filters use more than a single input in producing their output, these filters inherit from Filter2to1, Filter3to1, Filter4to1 etc.
One such filter is `DirectionalBlur` which has image and direction field inputs. In the following example we use an aside to 
draw a direction field which is fed into the blur filter.

```kotlin
fun main() = application {
    program {
        val composite = compose {
            val directionField = aside {
                draw {
                    // [...]
                }
            }
            layer {
                draw {
                    // [...]
                }
                post(DirectionalBlur(), directionField)
            }
        }
        extend {
            // draw the composite
            composite.draw(drawer)
        }
    }
}
```

##### Example

```kotlin
import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.edges.EdgesWork
import org.openrndr.extra.gui.GUI
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 768
        height = 768
    }
    program {
        val w2 = width / 2.0
        val h2 = height / 2.0

        val c = compose {
            draw {
                drawer.fill = ColorRGBa.PINK
                drawer.circle(width / 2.0, height / 2.0, 10.0)
            }

            layer {
                blend(Add())

                draw {
                    drawer.circle(width / 2.0, height / 2.0, 100.0)
                }
                post(ApproximateGaussianBlur()) {
                    window = 10
                    sigma = Math.cos(seconds * 10.0) * 10.0 + 10.0
                }
            }
        }
        extend(gui)
        extend {
            c.draw(drawer)
        }
    }
}
```
<!-- __demos__ -->
## Demos
### DemoAside01

Demonstrates how to reuse a layer in the Compositor by using `aside { }`.

The `aside` block can make use of `draw`, `mask` and `post`. In this demo
only the latter is used to apply a full-window animated `Checkers` effect.
The `aside` is not displayed by default.

Next, a white, centered circle is drawn.

Finally, a `HashBlurDynamic` post-processing effect is applied. The dynamic
version of the HashBlur effect multiplies its `radius` argument by the red component
of the provided texture (containing the animated checkers in this case).

![DemoAside01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-compositor/images/DemoAside01Kt.png)

[source code](src/jvmDemo/kotlin/DemoAside01.kt)

### DemoCompositor01

Compositor demo showing 3 layers of moving items
with a different amount of blur in each layer,
simulating depth of field

![DemoCompositor01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-compositor/images/DemoCompositor01Kt.png)

[source code](src/jvmDemo/kotlin/DemoCompositor01.kt)

### DemoCompositor02

Demonstration of using [BufferMultisample] on a per layer basis.
Try changing which layer has multisampling applied and observe the results.

![DemoCompositor02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-compositor/images/DemoCompositor02Kt.png)

[source code](src/jvmDemo/kotlin/DemoCompositor02.kt)
