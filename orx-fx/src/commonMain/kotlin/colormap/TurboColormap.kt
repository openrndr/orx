@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.colormap

import org.openrndr.extra.fx.fx_turbo_colormap
import org.openrndr.extra.parameters.Description

/**
 * Maps values of the RED color channel to Turbo Colormap according to
 * [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 * by Google.
 */
@Description("turbo colormap")
open class TurboColormap : ColormapFilter(fx_turbo_colormap, "turbo-colormap")
