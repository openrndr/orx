package org.openrndr.extra.runway

import com.google.gson.annotations.SerializedName

class CaptionRequest(val caption: String)
class CaptionResult(val result: String)

class BigBiGANQuery(@SerializedName("input_image") val inputImage: String)
class BigBiGANResult(@SerializedName("output_image") val outputImage: String)
