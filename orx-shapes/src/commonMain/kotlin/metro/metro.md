# Metro map schematization

`Shape.metro()` (in `Metro.kt`, package `org.openrndr.extra.shapes.metro`) replaces every contour
with a polyline whose segments all point in one of a few allowed directions, as in a metro map.

```kotlin
val octilinear = shape.metro(directions = 4)                // 0°, 45°, 90°, 135°
val rectilinear = contour.metro(directions = 2)             // horizontal and vertical only
val simpler = contour.metro(segmentPenalty = 100.0, boundsWeight = 1.0)
val vertices = points.metro(closed = false, directions = 3) // works on plain points too
```

It implements the heuristic from T. Dwyer, N. Hurst and D. Merrick, *A Fast and Simple Heuristic
for Metro Map Path Simplification* (ISVC 2008, LNCS 5359, pp. 22–30). The lib2geom toy
`src/toys/metro.cpp` (`fit::schematised_merging`), written by two of the authors, was a second
reference.

## Entry points

| Function | Returns |
|----------|---------|
| `Shape.metro(...)` | a `Shape` with every contour schematized independently |
| `ShapeContour.metro(...)` | a linear `ShapeContour` with the same `closed` and `polarity` |
| `List<Vector2>.metro(closed, ...)` | the polyline vertices; for closed input the first vertex is not repeated |

The contour functions linearize the input and then call the list version.

## Parameters

```kotlin
fun ShapeContour.metro(
    directions: Int = 4,
    angleOffset: Double = 0.0,
    segmentPenalty: Double = 0.0,
    boundsWeight: Double = 0.0,
    distanceTolerance: Double = 0.5,
    samplingDistance: Double = Double.POSITIVE_INFINITY
): ShapeContour
```

- `directions`: the number of allowed directions, spread evenly over 180°. Direction `i` is at
  `angleOffset + 180° · i / directions`. 2 is rectilinear, 3 hexilinear, 4 octilinear. The paper
  counts the same way (|C|); Merrick and Gudmundsson count both senses of each direction, so their
  octilinear is 8.
- `angleOffset`: rotates the whole set of directions, in degrees.
- `segmentPenalty`: a cost per output segment, in squared distance units. Larger values give
  fewer segments. 0 (the default) disables it.
- `boundsWeight`: weight of the segment-length error (the paper's γ, see
  [Greedy merging](#4-greedy-merging-section-4)). 0 (the default) disables it.
- `distanceTolerance`: passed to `ShapeContour.sampleLinear` to flatten curved segments.
- `samplingDistance`: when finite, every linear segment is subdivided so that points are at most
  this far apart. The default uses only the linearized vertices.

With both `segmentPenalty` and `boundsWeight` at 0 you get the paper's basic algorithm (section 3):
only adjacent parallel blocks are merged.

### Choosing the points

Every input point has equal weight in the least-squares fits, so the choice of points matters:

- For a polyline whose vertices mean something (stations on a metro line), keep the default
  `samplingDistance`. Only the vertices are fitted, as in the paper.
- For curved contours, `sampleLinear` already gives fairly even points along the curve.
- Set `samplingDistance` to weight the contour by length instead of by vertex. Then long edges
  pull harder than short ones.

The regression cost grows with the number of points. So a suitable `segmentPenalty` depends on both
the scale of the input and its point density.

## Pipeline

### 1. Preparation (`List<Vector2>.metro`)

- Consecutive duplicate points are removed. For closed input this includes the last point if it
  equals the first. A zero-length block would fit every direction equally well.
- Fewer than 2 points (3 for closed input) are returned unchanged.
- The points are translated so their mean is at the origin and translated back at the end. The
  fits use sums of squared coordinates, which lose precision far from the origin.
- The unit direction vectors are built from `directions` and `angleOffset`.

### 2. Blocks and their fits (`MetroFit.fit`)

A *block* is a run of consecutive points `start..end` (inclusive). Adjacent blocks share a point: the
last point of one block is the first point of the next.

Each block is fitted with a line through its barycenter **b**, in one of the allowed directions. For
a direction **d** with normal **n**, the regression cost is the paper's equation 1:

```
regression(V) = Σ ((a − b) · n)²       for a ∈ V
              = n_x² C_xx + n_y² C_yy + 2 n_x n_y C_xy
```

Here `C` holds the central second moments, for example `C_xx = S_xx − S_x² / |V|`. The block's
direction is the one with the lowest cost. Ties go to the lowest index.

Theorem 1 of the paper says these sums give the cost of any direction in constant time. The code
keeps prefix sums of `S_x, S_y, S_xx, S_yy, S_xy`. The statistics of any range are then one
subtraction, and fitting a block costs O(|C|) whatever its size. For closed paths the prefix arrays
cover the points twice, so a block that wraps past the last point is still one contiguous range.

Blocks are immutable `Block` objects with `prev`/`next` links: a doubly linked list, circular for
closed paths. Merging blocks `a` and `b` fits a new block over `a.start .. a.start + |a| + |b|`.
The new block takes their place in the list, and `a` and `b` are marked dead.

The paper takes `V' = V₁ ∪ V₂`, so the shared point is counted once. The lib2geom toy adds the two
blocks' statistics, which counts the shared point twice. It also chooses a block's direction using
a line through the block's first point instead of the barycenter. This implementation follows the
paper on both points.

### 3. Parallel merging (section 3, `mergeParallel`)

The path starts as one block per input edge. That is n − 1 blocks for open paths and n for closed
ones. Then adjacent blocks with the same direction are merged until none remain. A merged block is
re-fitted, and its new direction can match one of its new neighbours, which then merges in turn.
This is the backtracking described in the paper.

The paper does this in O(|C| n) with a single pass and a backtrack step. The code repeats passes
over the list until nothing changes. That is simpler, and fast enough for contour-sized inputs.

### 4. Greedy merging (section 4, `mergeGreedy`)

Only when `segmentPenalty > 0` or `boundsWeight > 0`. The cost of the whole path is

```
cost = Σ over blocks B of ( segmentPenalty + regression(B) + boundsWeight · bounds(B) )
```

This is the paper's equation 2 with β = 1 and α = `segmentPenalty` · n. The paper writes the
first term as α·m/n for m blocks.

`bounds(B)` (the paper's *bb_cost*) measures how well the drawn segment covers the block's points
along the line:

- The direction **d** is oriented from the block's first point towards its last.
- `lo` and `hi` are the smallest and largest projection of the block's points onto **d**.
- `t0` and `t1` are the projections of the segment's actual start and end, which depend on the
  neighbouring blocks.
- `bounds(B) = (t0 − lo)² + (t1 − hi)²`.

This penalizes segments that are too short to reach their points, and segments that overshoot them.
It is what turns the paper's figure 5 into figure 6.

Each round computes, for every adjacent pair `(a, b)`, the change in cost if they were merged into
`m`. A merge changes the endpoints of the neighbours `a.prev` and `b.next`, so their bounds terms
are part of the local comparison:

```
before = cost(a.prev) + cost(a) + cost(b) + cost(b.next)
after  = cost(a.prev) + cost(m) + cost(b.next)        // neighbours re-evaluated against m
```

The pair with the most negative change is merged. Parallel merging then runs again; the paper gives
parallel pairs infinite priority. Rounds repeat until no merge lowers the cost.

The paper uses a priority queue for O(n log n). Here each round rescans all pairs, and bounds terms
scan their block's points. That makes it roughly O(m · n) overall, which is fine at contour scale.
Like the paper's version, the result is a local minimum, not an optimum.

### 5. Output vertices (`solve`, `junction`)

- **Between adjacent blocks** with different directions, the vertex is the intersection of their
  lines. With discrete directions at least 180°/|C| apart this is always well-conditioned.
- **First and last vertex of an open path:** the first point projected onto the first block's line,
  and the last point projected onto the last block's line.
- **Closed path:** only junctions, all the way round.
- **Parallel neighbours.** Two adjacent blocks can still have the same direction when the 3-block
  minimum stops a merge (see below). Then the shared point is projected onto both lines and both
  projections are emitted. This gives a short jog perpendicular to the lines. If the projections
  coincide, one vertex is emitted.

## Closed contours

The paper and the toy only handle open paths. For closed contours:

- The block list is circular, and blocks may wrap past the last point. This is why the prefix sums
  are doubled.
- There is no first or last point; every vertex is a junction.
- Merging never goes below **3 blocks**. Two lines meet in a single point, so a closed path needs at
  least three. Near that limit, parallel neighbours can remain; those are joined with jogs, as
  described above. In practice this only happens with thin shapes and very few directions.

Winding is preserved, because the output vertices follow the input order.

## Tests and demo

Tests (`commonTest/kotlin/TestMetro.kt`):

- **Rectangle**, `directions = 2`: 4 segments, with the corners reproduced within 1e-6.
- **Circle** r = 100, `directions = 4`: an octagon, with all segments at multiples of 45° and the
  vertices within 15 of the circle.
- **Noisy open path**, `directions` 2 to 6: every segment is in an allowed direction, both with and
  without greedy merging. Greedy merging never gives more vertices than parallel merging alone.
- **Straight diagonal line:** two vertices, at the first and last point.

`DemoMetro01` draws a Hobby-curve blob with a hole and a noisy open path, with directions 2, 3, 4
and 6. The bottom row shows the same path with `segmentPenalty = 100.0, boundsWeight = 1.0`.

## Known limitations

- **Contours are schematized independently.** Contours that share points (lines through the same
  station) are not aligned with each other. Topology is not preserved, so schematized contours, or
  parts of one contour, can cross where the input did not. Section 5 of the paper leaves this open
  too.
- **Station order along a segment is not enforced.** The paper drops this constraint as well. The
  nearest points on the output can be out of order relative to the input.
- **The direction of a block is chosen by regression alone,** even during greedy merging. Choosing
  it by the full local cost could sometimes do better.
- **No splitting.** The paper's suggested refinement of splitting blocks again is not implemented.
- **The output is always linear;** curved input segments are not kept.
- **Scale-dependent parameters.** `segmentPenalty` is in squared distance units and the regression
  cost sums over points. A value tuned for one shape does not transfer to a shape of a different
  size or sampling density.
