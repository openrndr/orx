package org.openrndr.extra.runway

import com.google.gson.annotations.SerializedName

// -- AttnGAN
class AttnGANRequest(val caption: String)

class AttnGANResult(val result: String)

// -- BDCN
class BdcnRequest(val input_image: String)

class BdcnResult(val output_image: String)

// -- BigBiGAN
class BigBiGANQuery(@SerializedName("input_image") val inputImage: String)

class BigBiGANResult(@SerializedName("output_image") val outputImage: String)

// -- DensePose
class DensePoseQuery(@SerializedName("input") val input: String)

class DensePoseResult(@SerializedName("output") val output: String)

// -- SPADE-COCO
class SpadeCocoRequest(val semantic_map: String)

class SpadeCocoResult(val output: String)

// -- GPT-2
class Gpt2Request(val prompt: String, val seed: Int = 0, @SerializedName("sequence_length") val sequenceLength: Int = 128)

class Gpt2Result(val text: String)

// -- im2txt
class Im2txtRequest(val image: String)

class Im2txtResult(val caption: String)

// -- PSENet
class PsenetRequest(@SerializedName("input_image") val inputImage: String)

class PsenetResult(val bboxes: Array<Array<Double>>)

// -- Face landmarks
class FaceLandmarksRequest(val photo: String)

class FaceLandmarksResponse(val points: List<List<Double>>, val labels: List<String>)

// -- StyleGAN

/**
 * StyleGAN request
 * @param z a list of 512 doubles
 */
class StyleGANRequest(val z: List<Double>, val truncation: Double = 1.0)

class StyleGANResponse(val image: String)

// -- DeOldify
class DeOldifyRequest(val image: String, val renderFactor: Int = 20)

class DeOldifyResponse(val image: String)

// -- DenseCap

class DenseCapRequest(val image: String, @SerializedName("max_detections") val maxDetections: Int = 10)
class DenseCapResponse(val bboxes: List<List<Double>>, val classes: List<String>, val scores: List<Double>)