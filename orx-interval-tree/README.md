# orx-interval-tree

For querying a data set containing time segments (start time and end time)
when we need all entries containing a specific time value. Useful when creating a timeline.

For more information on interval trees read the [wikipedia page](https://en.wikipedia.org/wiki/Interval_tree).

## Usage
```
// -- the item class we want to search for
class Item(val start: Double, val end: Double)

// -- the items we want to search in
val items = List(100000) { Item(Math.random(), 1.0 + Math.random()) }

// -- build the interval tree, note how buildIntervalTree accepts a function that returns the start and end of the interval.
val tree = buildIntervalTree(items) {
    Pair(it.start, it.end)
}

// -- search for all items that intersect 0.05
val results = tree.queryPoint(0.05)
```

