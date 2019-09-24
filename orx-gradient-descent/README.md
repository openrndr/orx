# orx-gradient-descent

A gradient descent based minimizer that is incredibly
easy to use.

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