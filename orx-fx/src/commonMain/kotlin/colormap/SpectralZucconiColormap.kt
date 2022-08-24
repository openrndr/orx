package org.openrndr.extra.fx.colormap

import org.openrndr.extra.fx.fx_spectral_zucconi_colormap
import org.openrndr.extra.parameters.Description

/**
 * Maps values of the RED color channel to natural light dispersion spectrum as described
 * by Alan Zucconi in the
 * [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 * article.
 */
@Description("spectral colormap")
class SpectralZucconiColormap : ColormapFilter(fx_spectral_zucconi_colormap, "spectral-zucconi-colormap")
