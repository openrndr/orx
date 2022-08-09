package org.openrndr.extra.fx.colormap

import org.openrndr.extra.fx.fx_grayscale_colormap
import org.openrndr.extra.parameters.Description

/**
 * Maps values of the RED color channel to grayscale.
 */
@Description("grayscale colormap")
class GrayscaleColormap : ColormapFilter(fx_grayscale_colormap, "grayscale-colormap")
