@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.colormap

import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.fx.fx_spectral_zucconi_colormap
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.shaderphrases.preprocess

/**
 * Maps values of the RED color channel to natural light dispersion spectrum as described
 * by Alan Zucconi in the
 * [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 * article.
 *
 * @see ColormapPhraseBook.spectralZucconi6
 * @see org.openrndr.extra.color.colormaps.spectralZucconi6
 */
@Description("spectral colormap")
class SpectralZucconiColormap : ColormapFilter(
    code = run {
        ColormapPhraseBook.register()
        fx_spectral_zucconi_colormap.preprocess()
    },
    name = "spectral-zucconi-colormap"
)
