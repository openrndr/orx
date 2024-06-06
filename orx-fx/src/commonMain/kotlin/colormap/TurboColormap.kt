@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.colormap

import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.fx.fx_turbo_colormap
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.shaderphrases.preprocess

/**
 * Maps values of the RED color channel to Turbo Colormap according to
 * [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 * by Google.
 *
 * @see ColormapPhraseBook.spectralZucconi6
 * @see org.openrndr.extra.color.colormaps.spectralZucconi6
 */
@Description("turbo colormap")
class TurboColormap : ColormapFilter(
    code = run {
        ColormapPhraseBook.register()
        fx_turbo_colormap.preprocess()
    },
    name = "turbo-colormap"
)
