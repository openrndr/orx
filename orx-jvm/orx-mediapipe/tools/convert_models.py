# /// script
# requires-python = ">=3.11,<3.13"
# dependencies = ["tf2onnx", "tensorflow", "onnx"]
# ///
"""
Downloads the MediaPipe models that orx-mediapipe uses and converts them from TFLite to ONNX, and downloads the
images of MediaPipe that the tests use.

    uv run orx-jvm/orx-mediapipe/tools/convert_models.py [output folder]

The output folder is orx-jvm/orx-mediapipe/models by default. The models are those of
https://ai.google.dev/edge/mediapipe/solutions/guide, under the Apache License 2.0.
"""
import io
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

MODELS = "https://storage.googleapis.com/mediapipe-models"
ASSETS = "https://storage.googleapis.com/mediapipe-assets"

# a single model, or the models in a task bundle
TFLITE = {
    "blaze_face_short_range": f"{MODELS}/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite",
    "selfie_segmenter": f"{MODELS}/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite",
    "selfie_segmenter_landscape": f"{MODELS}/image_segmenter/selfie_segmenter_landscape/float16/latest/selfie_segmenter_landscape.tflite",
    "selfie_multiclass_256x256": f"{MODELS}/image_segmenter/selfie_multiclass_256x256/float32/latest/selfie_multiclass_256x256.tflite",
    "magic_touch": f"{MODELS}/interactive_segmenter/magic_touch/float32/latest/magic_touch.tflite",
}
# the models in a task bundle, by their names in the bundle and the names they are saved as
TASKS = {
    f"{MODELS}/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task":
        {"hand_detector": "hand_detector", "hand_landmarks_detector": "hand_landmarks_detector"},
    f"{MODELS}/face_landmarker/face_landmarker/float16/latest/face_landmarker.task":
        {"face_landmarks_detector": "face_landmarks_detector", "face_blendshapes": "face_blendshapes"},
    f"{MODELS}/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task":
        {"pose_detector": "pose_detector", "pose_landmarks_detector": "pose_landmarks_detector_lite"},
    f"{MODELS}/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task":
        {"pose_landmarks_detector": "pose_landmarks_detector_full"},
}
IMAGES = ["portrait.jpg", "thumb_up.jpg", "pointing_up.jpg", "right_hands.jpg", "pose.jpg"]


def fetch(url: str) -> bytes:
    print(f"downloading {url}")
    with urllib.request.urlopen(url) as r:
        return r.read()


def densify(tflite: Path):
    """
    Stores the sparse weights of the model densely, which tf2onnx cannot read, and removes the DENSIFY operators that
    made them dense. Only sparse weights whose last dimension is compressed (CSR), without blocks, are read.
    """
    import numpy as np
    from tensorflow.lite.python import schema_py_generated as schema
    from tensorflow.lite.tools import flatbuffer_utils

    model = flatbuffer_utils.read_model(str(tflite))
    graph = model.subgraphs[0]
    densify_code = [i for i, c in enumerate(model.operatorCodes)
                    if max(c.builtinCode, c.deprecatedBuiltinCode) == schema.BuiltinOperator.DENSIFY]
    operators = [o for o in graph.operators if o.opcodeIndex in densify_code]
    if not operators:
        return
    print(f"densifying {tflite.name}")
    dtypes = {schema.TensorType.FLOAT16: np.float16, schema.TensorType.FLOAT32: np.float32}

    def array(vector):
        # the values of a Int32Vector, Uint16Vector or Uint8Vector
        return np.asarray(vector.values)

    for o in operators:
        sparse = graph.tensors[o.inputs[0]]
        sparsity = sparse.sparsity
        dims = sparsity.dimMetadata
        if (sparsity.blockMap is not None and len(sparsity.blockMap) > 0) or \
                list(sparsity.traversalOrder) != list(range(len(dims))) or \
                any(d.format != schema.DimensionType.DENSE for d in dims[:-1]) or \
                dims[-1].format != schema.DimensionType.SPARSE_CSR:
            raise ValueError(f"{sparse.name}: sparsity that is not read")
        shape = [int(v) for v in sparse.shape]
        values = np.frombuffer(bytes(model.buffers[sparse.buffer].data), dtype=dtypes[sparse.type])
        segments = array(dims[-1].arraySegments)
        indices = array(dims[-1].arrayIndices)
        dense = np.zeros((int(np.prod(shape[:-1])), shape[-1]), dtype=values.dtype)
        for row in range(dense.shape[0]):
            for k in range(segments[row], segments[row + 1]):
                dense[row, indices[k]] = values[k]
        # the sparse input and the output both hold the dense weights
        for index in (o.inputs[0], o.outputs[0]):
            tensor = graph.tensors[index]
            tensor.sparsity = None
            buffer = schema.BufferT()
            buffer.data = np.frombuffer(dense.tobytes(), dtype=np.uint8)
            model.buffers.append(buffer)
            tensor.buffer = len(model.buffers) - 1
    graph.operators = [o for o in graph.operators if o.opcodeIndex not in densify_code]
    flatbuffer_utils.write_model(model, str(tflite))


def replace_transpose_convolutions(tflite: Path):
    """
    Replaces MediaPipe's custom Convolution2DTransposeBias operators, which tf2onnx cannot convert, by TRANSPOSE_CONV
    operators with a bias. Their custom options are the padding and the strides, three 32 bit integers.
    """
    import numpy as np
    from tensorflow.lite.python import schema_py_generated as schema
    from tensorflow.lite.tools import flatbuffer_utils

    model = flatbuffer_utils.read_model(str(tflite))
    custom = [i for i, c in enumerate(model.operatorCodes) if c.customCode == b"Convolution2DTransposeBias"]
    if not custom:
        return
    print(f"replacing the transpose convolutions of {tflite.name}")
    code = schema.OperatorCodeT()
    code.builtinCode = schema.BuiltinOperator.TRANSPOSE_CONV
    code.deprecatedBuiltinCode = schema.BuiltinOperator.TRANSPOSE_CONV
    # the version with a bias
    code.version = 3
    model.operatorCodes.append(code)
    for graph in model.subgraphs:
        for o in graph.operators:
            if o.opcodeIndex not in custom:
                continue
            padding, stride_w, stride_h = np.frombuffer(bytes(bytearray(o.customOptions)), dtype=np.int32)[:3]
            if padding != 1:
                raise ValueError(f"{tflite.name}: transpose convolution with padding {padding}, not SAME")
            # the shape of the output, a constant
            buffer = schema.BufferT()
            buffer.data = np.frombuffer(np.asarray(graph.tensors[o.outputs[0]].shape, dtype=np.int32).tobytes(), dtype=np.uint8)
            model.buffers.append(buffer)
            shape = schema.TensorT()
            shape.shape = np.asarray([4], dtype=np.int32)
            shape.type = schema.TensorType.INT32
            shape.buffer = len(model.buffers) - 1
            shape.name = graph.tensors[o.outputs[0]].name + b"/output_shape"
            graph.tensors.append(shape)
            options = schema.TransposeConvOptionsT()
            options.padding = schema.Padding.SAME
            options.strideW = int(stride_w)
            options.strideH = int(stride_h)
            # the input, the weights and the bias, after the shape of the output and in the order of TRANSPOSE_CONV
            source, weights, bias = o.inputs
            o.inputs = np.asarray([len(graph.tensors) - 1, weights, source, bias], dtype=np.int32)
            o.opcodeIndex = len(model.operatorCodes) - 1
            o.builtinOptionsType = schema.BuiltinOptions.TransposeConvOptions
            o.builtinOptions = options
            o.customOptions = None
    flatbuffer_utils.write_model(model, str(tflite))


def convert(tflite: Path, onnx: Path):
    densify(tflite)
    replace_transpose_convolutions(tflite)
    print(f"converting {tflite.name}")
    subprocess.run([sys.executable, "-m", "tf2onnx.convert", "--tflite", str(tflite), "--output", str(onnx), "--opset", "17"],
                   check=True, capture_output=True)


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "models"
    tflite_dir = out / "tflite"
    tflite_dir.mkdir(parents=True, exist_ok=True)

    for name, url in TFLITE.items():
        (tflite_dir / f"{name}.tflite").write_bytes(fetch(url))
    for url, names in TASKS.items():
        with zipfile.ZipFile(io.BytesIO(fetch(url))) as task:
            for name, saved in names.items():
                (tflite_dir / f"{saved}.tflite").write_bytes(task.read(f"{name}.tflite"))
    for tflite in sorted(tflite_dir.glob("*.tflite")):
        convert(tflite, out / f"{tflite.stem}.onnx")

    images = out / "images"
    images.mkdir(exist_ok=True)
    for name in IMAGES:
        (images / name).write_bytes(fetch(f"{ASSETS}/{name}"))
    print(f"models in {out}")


if __name__ == "__main__":
    main()
