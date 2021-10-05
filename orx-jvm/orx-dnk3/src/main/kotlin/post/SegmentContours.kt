package org.openrndr.extra.dnk3.post

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.resourceUrl

class SegmentContoursMSAA8 : Filter(filterShaderFromUrl(resourceUrl("/shaders/segment-contours-msaa-8.frag")))
class SegmentContours : Filter(filterShaderFromUrl(resourceUrl("/shaders/segment-contours.frag")))
