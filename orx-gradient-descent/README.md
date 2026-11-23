# orx-gradient-descent

Finds equation inputs that output a minimum value: easy to use gradient descent based minimizer. 

## Usage

```kotlin
// define a model
class Model {
    var x = 0.0
    var y = 0.0 
}

val model = Model()
minimizeModel(model) { m ->
    (m.x-4.0)*(m.x-4.0) + (m.y-3.0)*(m.y-3.0)
}

// model.x is close to 4 and model y is close to 3 at this point
```

## Based on g9

orx-gradient-descent is based on [g9](https://omrelli.ug/g9/) ([source](https://github.com/bijection/g9), MIT
license), a JavaScript library for interactive graphics. `gradient` and `minimize` are a Kotlin port of g9's
minimizer, `minimize.js`, a quasi-Newton (BFGS) minimizer with a backtracking line search, which g9 adapted from the
`gradient` and `uncmin` functions of [numeric.js](https://github.com/sloisel/numeric) by Sébastien Loisel (MIT
license).

The demos also follow g9's idea of direct manipulation: a drawing is computed from a few parameters, and dragging any
point of the drawing changes the parameters, by minimizing the distance from that point to the mouse, such that the
point follows the mouse and the rest of the drawing follows the parameters. `DragParameters`, in the demos, does this
for any drawing and adds to it:
- `affects`, the parameters that dragging a point may change, as g9's `affects` option does;
- `damping`, which prefers the smallest change of the parameters, for drawings of more parameters than a point needs;
- `penalty`, a term of the parameters minimized with the distance, for constraints that dragging keeps;
- `grab`, which lets a drawing be dragged by any place, not only by its points.

The demos after examples of the [g9 gallery](https://omrelli.ug/g9/gallery/) are `DemoDragMirror01`,
`DemoDragRings01`, `DemoDragBezier01`, `DemoDragCircle01`, `DemoDragCube01`, `DemoDragDragon01`, `DemoDragTree01`,
`DemoDragTurtle01`, `DemoDragGridWarp01`, `DemoDragCrank01`, `DemoDragAffects01`, `DemoDragClamp01`, `DemoDragClock01`
and `DemoDragImage01`; the others take the idea further, to hyperbolic and spherical geometry, Möbius
transformations, linkages, inverse kinematics, fitting, letters, tilings, quasicrystals and magnetic fields.

## Data binding

Currently we support minimizing model classes that contain 
`Double`, `Vector2`, `Vector3` and `Vector4` typed properties, 
other types are silently ignored.

An example of a supported model:
```kotlin
class Model {
    var x = 0.0
    var y = 0.0
    var v2 = Vector2.ZERO
}
```
