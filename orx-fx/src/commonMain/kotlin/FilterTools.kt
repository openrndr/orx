package org.openrndr.extra.fx

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Shader
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.internal.Driver
import org.openrndr.resourceUrl

fun mppFilterShader(code: String, name: String) : Shader = filterShaderFromCode(code, name, includeShaderConfiguration = true)

internal data class ColorBufferDescription(val width: Int, val height: Int, val contentScale: Double, val format: ColorFormat, val type: ColorType)
