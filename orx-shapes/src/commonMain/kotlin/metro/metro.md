# Metro map schematization

`Shape.metro()` (in `Metro.kt`, package `org.openrndr.extra.shapes.metro`) replaces every contour
with a polyline whose segments all point in one of a few allowed directions, as in a metro map.

```kotlin
val octilinear = shape.metro(directions = 4)                // 0°, 45°, 90°, 135°
val rectilinear = contour.metro(directions = 2)             // horizontal and vertical only
val simpler = contour.metro(segmentPenalty = 100.0, boundsWeight = 1.0)
val vertices = points.metro(closed = false, directions = 3) // works on plain points too
val rounded = vertices.filletCorners(15.0, closed = false) // corners become circular arcs
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

The contour functions linearize the input and call the list version. The output has sharp corners;
to round them, pass the vertices to `List<Vector2>.filletCorners` (see [Corner arcs](#6-corner-arcs)).

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

### 3. Forced merging (section 3, `mergeForced`)

The path starts as one block per input edge. That is n − 1 blocks for open paths and n for closed
ones. Then two kinds of invalid configurations are merged away until none remain:

- **Parallel neighbours.** Adjacent blocks with the same direction are merged, as in the paper.
- **Reversed segments** (`reversed`), an addition to the paper. A block's segment runs from its
  junction with the previous block to its junction with the next. With 3 or more directions, a
  short block between two neighbours can have those junctions in the wrong order along its line.
  For example, a 2-point horizontal block sits between a diagonal and a vertical whose lines cross
  beyond it. The segment then runs against the order of its points: the output overshoots, doubles
  back along the short segment and crosses itself in a small X-shaped loop that the input doesn't
  have. Such a block is merged with whichever neighbour increases the regression cost least.

  The test is: with **d** oriented from the block's first point towards its last, the segment
  is reversed if `end · d < start · d`. The paper's `bounds` term (step 4) penalizes this only
  softly, and only when `boundsWeight > 0`.

A merged block is re-fitted, and its new direction can match one of its new neighbours, which then
merges in turn. This is the backtracking described in the paper. A merge also moves the junctions
of the neighbouring blocks, so they are checked for reversal again.

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

The pair with the most negative change is merged. Forced merging then runs again; the paper gives
parallel pairs infinite priority, and reversed segments are treated the same way. Rounds repeat until no merge lowers the cost.

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

### 6. Corner arcs

The metro functions don't round corners themselves. Rounding is a separate post-process on the
output vertices, `List<Vector2>.filletCorners(radius, closed, reverseCurves = true, polarity)` in
`operators/FilletCorners.kt`. It knows nothing about the fit, so it works on any polyline. Most
corners get a fillet; opposite corners that are too close for two fillets become an S-bend of two
opposite arcs, unless `reverseCurves = false`, and arcs that don't fit next to their neighbours
shrink. It returns
a `ShapeContour` because arcs need curved segments. It uses two kinds of rounding: single-corner
fillets and two-corner S-bends.

**Fillets.** For a corner P with incoming direction **u**, outgoing direction **v** and turning
angle θ:

- The arc starts at `A = P − u·t` and ends at `B = P + v·t`, where the tangent length is
  `t = r · tan(θ / 2)`.
- Its center lies at distance r from A, perpendicular to **u**, on the side the path turns to.

**S-bends (reverse curves).** Take two consecutive corners i and i+1 that turn in opposite
directions, such as a jog between parallel lines or a short diagonal step. When their fillets don't
fit on the edge between them (`t_i + t_{i+1} > L`), the pair is replaced by two arcs of the full
radius r. The arcs turn in opposite directions and meet tangentially at an inflection point T. The
incoming and outgoing edges are kept, and the middle edge is replaced. Fillets would instead have to
shrink to `r · L / (t_i + t_{i+1})`, which turns a short jog into a near-sharp kink.

`reverseCurve` solves for the S-bend:

- The center C1 lies on the line parallel to the incoming edge at distance r, on the side corner i
  turns to: `C1 = K + u·a`. Likewise `C2 = L + w·b` for the outgoing edge.
- T, the midpoint of C1 and C2, must lie on the line of the middle edge. This is linear, so b is a
  linear function of a.
- `|C2 − C1| = 2r` makes the arcs touch. This is a quadratic in a.

A root is valid if the entry lies at or before corner i (`a ≤ 0`), the exit lies at or after corner
i+1 (`b ≥ 0`), and both arcs sweep between 0 and 180°. Of the valid roots, the one whose T is closest
to where the two fillets would meet is used. When the fillets fit exactly, both fillets touch the
middle edge at the same point. That configuration satisfies the same equations, so the change from
fillets to an S-bend is continuous. The tests check that the contours agree within 1e-4 on either
side of the switch.

Pairs are chosen greedily, most overfull middle edge first, and no corner is used twice. In a
staircase of short jogs between longer runs, every jog becomes an S-bend. In a zigzag where every
edge is too short, the pairing alternates: every other edge becomes the middle of an S-bend, and the
edges in between connect consecutive S-bends. Corners that turn the same way (a U-turn over a short edge)
always get fillets.

**Arcs.** Each arc is split into ⌈angle / 90°⌉ cubic Béziers. Each piece of angle φ has its handles
along the tangents at both ends, with length `4/3 · tan(φ / 4) · r`. The radial error is below 0.03%
of r. Each arc meets the straight edges, and the other arc at the inflection, with exactly the
shared tangent. That makes the whole contour G1 continuous: the tangent direction never jumps.
Curvature does jump: from 0 on a straight edge to 1/r on an arc, and from +1/r to −1/r at an
inflection.

**Sharing edges.** Each rounding claims a length on the edges it touches:

| Rounding | Claim |
|----------|-------|
| Fillet | t on both edges |
| S-bend | −a on the incoming edge, b on the outgoing edge; the middle edge is entirely its own |

When the two claims on an edge add up to more than its length L, the edge is divided in proportion to
the claims. This rules out overlapping arcs and backwards lines.

- A fillet scales its radius down to fit its share.
- An S-bend's claims grow nonlinearly with r, so the largest radius that fits is found by
  bisection. The search runs between the full radius and the radius at which the two fillets just
  fit on the middle edge. If even that doesn't fit, the pair falls back to two shrunk fillets.

For open paths the first and last edge hold only one rounding, so it may use the full edge length.

Gaps below `1e-6 · max(1, r)` between consecutive roundings are numerical, for example from the
bisection. No line is emitted there, because a line that short has no meaningful direction. Instead
the first Bézier piece is moved onto the previous end point, together with its first control point,
so its tangent is unchanged.

**Degenerate corners.** Duplicate vertices are dropped. Collinear corners (θ ≈ 0) need no arc.
Reversals (θ ≈ 180°) can't be filleted with a finite tangent length and stay sharp.

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
  without greedy merging. Greedy merging never gives more vertices than forced merging alone.
- **Straight diagonal line:** two vertices, at the first and last point.
- **No reversed-segment loops:** 100 random walks with a wandering heading; those that cross
  themselves are skipped, and at least 20 must remain. For directions 2 to 6, with and without
  greedy merging, no output segment i crosses segment i + 2, which is the signature of the
  overshoot loop. The test fails when the reversal check is disabled.

Tests for the corner arcs (`commonTest/kotlin/TestFilletCorners.kt`):

- **Square** 100 × 100, r = 10: 4 lines and 4 arcs, length 4·80 + 2π·10, and arc points 10 from the
  expected center within 3e-4 relative.
- **160° turn:** two Bézier pieces, both on the expected circle.
- **Zigzag** with r = 1000: every pair becomes an S-bend that must shrink to fit its neighbours. The
  endpoints are kept, and no line runs backwards.
- **Jog** of 10 between horizontal lines, r = 20: an S-bend of two arcs of radius 20 that turn
  opposite ways. The entry is at `100 − 20·sin(acos(0.75))`, the inflection at (100, 5) and the exit
  mirrored, all within 1e-9. With `reverseCurves = false` the result is two fillets of radius 5.
- **Switch between fillets and S-bend:** an octilinear step whose diagonal is 1e-6 shorter or
  longer than two fillets need. The contours agree within 1e-4 everywhere.
- **Closed staircase** of jogs: tangent continuous, and the result doesn't depend on which vertex
  comes first.
- **Degenerate corners:** duplicates, collinear points and a reversal stay linear.
- **Filleted metro output:** for random walks with directions 2 to 6 and r = 2, 10 and 1000,
  and for a closed circle, consecutive segments connect and their tangent directions agree within
  1e-9 at every joint.

`DemoMetro01` draws a Hobby-curve blob with a hole and a noisy open path, with directions 2, 3, 4
and 6. The bottom row shows the same path with `segmentPenalty = 100.0, boundsWeight = 1.0`.
`DemoMetro03` draws the same inputs octilinearly, filleted with radius 0, 5, 15 and 40. It also shows
an octilinear line with jogs, rounded with fillets only and with S-bends.

## Known limitations

- **Contours are schematized independently.** Contours that share points (lines through the same
  station) are not aligned with each other. Topology is not preserved, so schematized contours, or
  parts of one contour, can cross where the input did not. Section 5 of the paper leaves this open
  too. Only the local overshoot loops of reversed segments are removed (see step 3). Parts of a
  contour that pass close to each other in the input can still cross after schematization.
- **Station order along a segment is not enforced.** The paper drops this constraint as well. The
  nearest points on the output can be out of order relative to the input.
- **The direction of a block is chosen by regression alone,** even during greedy merging. Choosing
  it by the full local cost could sometimes do better.
- **No splitting.** The paper's suggested refinement of splitting blocks again is not implemented.
- **Curved input segments are not kept.** The fit is linear. `filletCorners` adds arcs only at
  corners, with one radius for all of them, apart from clamping.
- **Corner arcs are G1, not G2.** Curvature jumps where an arc meets a straight edge and at S-bend
  inflections. Rounding doesn't move the fit, so with large radii the path cuts inside the fitted
  corners, and an S-bend moves off its middle edge.
- **S-bend pairing is greedy.** In a zigzag where every edge is too short, only every other edge is
  the middle of an S-bend.
- **On dense, noisy paths** most corners are limited by their neighbours, so increasing the radius
  beyond the corner spacing changes little.
- **Scale-dependent parameters.** `segmentPenalty` is in squared distance units and the regression
  cost sums over points. A value tuned for one shape does not transfer to a shape of a different
  size or sampling density.
