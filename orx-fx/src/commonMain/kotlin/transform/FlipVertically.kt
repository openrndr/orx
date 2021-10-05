package org.openrndr.extra.fx.transform

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_flip_vertically
import org.openrndr.extra.fx.mppFilterShader

/**
 * Vertically flips in the input image
 */
class FlipVertically : Filter(mppFilterShader(fx_flip_vertically, "flip-vertically"))