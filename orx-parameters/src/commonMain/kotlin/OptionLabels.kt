package org.openrndr.extra.parameters

/** [name], the name of a constant, in lowercase with spaces for underscores: `SOFT_LIGHT` is "soft light" */
fun optionLabelOf(name: String): String = name.lowercase().replace('_', ' ')
