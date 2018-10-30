# orx-integral-image

#### Usage

```
val image = colorBuffer( ... )
image.shadow.download()
val integralImage = IntegralImage.fromColorBufferShadow(image.shadow)

// -- the sum for a given area can be queried using
val sum = integralImage.sum(IntRectangle(20, 20, 100, 100))
```
