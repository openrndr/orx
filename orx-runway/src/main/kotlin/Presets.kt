package org.openrndr.extra.runway

import com.google.gson.annotations.SerializedName

// -- AttnGAN
class CaptionRequest(val caption: String)
class CaptionResult(val result: String)

// -- BDCN
class BdcnRequest(val input_image: String)
class BdcnResult(val output_image: String)

// -- BigBiGAN
class BigBiGANQuery(@SerializedName("input_image") val inputImage: String)
class BigBiGANResult(@SerializedName("output_image") val outputImage: String)

// -- SPADE-COCO
class SpadeCocoRequest(val semantic_map: String)
class SpadeCocoResult(val output: String)

// -- GPT-2
class Gpt2Request(val prompt: String)
class Gpt2Result(val text: String)

// -- im2txt
class Im2txtRequest(val image: String)
class Im2txtResult(val caption: String)

// -- PSENet
class PsenetRequest(@SerializedName("input_image") val inputImage: String)
class PsenetResult(val bboxes: Array<Array<Double>>)