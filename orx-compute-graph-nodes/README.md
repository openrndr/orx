# orx-compute-graph-nodes

A collection of nodes that can be used with `orx-computer-graph`.

## List of nodes

### Multi-platform

Name            | Description           | Inputs | Outputs
----------------|-----------------------|--------|---------
`filterNode`    | Wrap around a `Filter`|        | `image`
`fitImageNode`  | Fit image to window bounds | `image` | `image`

### JVM only

Name            | Description       | Inputs | Outputs
----------------|-------------------|--------|---------
`drawCacheNode` | Cache drawing in an internal color buffer, commonly used as the final stage node | | `image`
`dropImageNode` | Listen for window file drop events | | `image`



