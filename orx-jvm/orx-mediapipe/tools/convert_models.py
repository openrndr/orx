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
    "hair_segmenter": f"{MODELS}/image_segmenter/hair_segmenter/float32/latest/hair_segmenter.tflite",
    "efficientnet_lite0": f"{MODELS}/image_classifier/efficientnet_lite0/float32/latest/efficientnet_lite0.tflite",
    "efficientnet_lite2": f"{MODELS}/image_classifier/efficientnet_lite2/float32/latest/efficientnet_lite2.tflite",
    "mobilenet_v3_small": f"{MODELS}/image_embedder/mobilenet_v3_small/float32/latest/mobilenet_v3_small.tflite",
    "mobilenet_v3_large": f"{MODELS}/image_embedder/mobilenet_v3_large/float32/latest/mobilenet_v3_large.tflite",
    "efficientdet_lite0": f"{MODELS}/object_detector/efficientdet_lite0/float32/latest/efficientdet_lite0.tflite",
    "efficientdet_lite2": f"{MODELS}/object_detector/efficientdet_lite2/float32/latest/efficientdet_lite2.tflite",
    "ssd_mobilenet_v2": f"{MODELS}/object_detector/ssd_mobilenet_v2/float32/latest/ssd_mobilenet_v2.tflite",
}
# the models in a task bundle, by their names in the bundle and the names they are saved as
TASKS = {
    f"{MODELS}/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task":
        {"hand_detector": "hand_detector", "hand_landmarks_detector": "hand_landmarks_detector"},
    f"{MODELS}/face_landmarker/face_landmarker/float16/latest/face_landmarker.task":
        {"face_landmarks_detector": "face_landmarks_detector", "face_blendshapes": "face_blendshapes"},
    f"{MODELS}/gesture_recognizer/gesture_recognizer/float16/latest/gesture_recognizer.task":
        {"hand_gesture_recognizer.task/gesture_embedder": "gesture_embedder",
         "hand_gesture_recognizer.task/canned_gesture_classifier": "canned_gesture_classifier"},
    # the styles share the face detector and the landmarks detector of the face stylizer
    f"{MODELS}/face_stylizer/blaze_face_stylizer/float32/latest/face_stylizer_color_sketch.task":
        {"face_detector": "face_stylizer_face_detector", "face_landmarks_detector": "face_stylizer_landmarks_detector",
         "face_stylizer": "face_stylizer_color_sketch"},
    f"{MODELS}/face_stylizer/blaze_face_stylizer/float32/latest/face_stylizer_color_ink.task":
        {"face_stylizer": "face_stylizer_color_ink"},
    f"{MODELS}/face_stylizer/blaze_face_stylizer/float32/latest/face_stylizer_oil_painting.task":
        {"face_stylizer": "face_stylizer_oil_painting"},
    # the holistic landmarker shares its face models with the face landmarker and the face stylizer
    f"{MODELS}/holistic_landmarker/holistic_landmarker/float16/latest/holistic_landmarker.task":
        {"pose_detector": "holistic_pose_detector", "pose_landmarks_detector": "holistic_pose_landmarks_detector",
         "hand_landmarks_detector": "holistic_hand_landmarks_detector", "hand_roi_refinement": "hand_roi_refinement"},
    f"{MODELS}/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task":
        {"pose_detector": "pose_detector", "pose_landmarks_detector": "pose_landmarks_detector_lite"},
    f"{MODELS}/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task":
        {"pose_landmarks_detector": "pose_landmarks_detector_full"},
    f"{MODELS}/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task":
        {"pose_landmarks_detector": "pose_landmarks_detector_heavy"},
}
IMAGES = ["portrait.jpg", "thumb_up.jpg", "pointing_up.jpg", "right_hands.jpg", "pose.jpg", "victory.jpg", "fist.jpg",
          "male_full_height_hands.jpg"]


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


def freeze_variables_and_noise(tflite: Path):
    """
    Makes the face stylizers convertible: the resource variables, which a subgraph called once assigns, are made
    constants, the FusedBatchNormV3 custom operators, which normalize by the statistics of their input, are made of
    builtin operators, and the noise of the RANDOM_STANDARD_NORMAL operators becomes inputs of the model, `noise_0`,
    `noise_1`, ...
    """
    import numpy as np
    from flatbuffers import flexbuffers
    from tensorflow.lite.python import schema_py_generated as schema
    from tensorflow.lite.tools import flatbuffer_utils

    model = flatbuffer_utils.read_model(str(tflite))
    ops = schema.BuiltinOperator

    def name_of(o):
        c = model.operatorCodes[o.opcodeIndex]
        return c.customCode.decode() if c.customCode else max(c.builtinCode, c.deprecatedBuiltinCode)

    graph = model.subgraphs[0]
    if not any(name_of(o) == ops.CALL_ONCE for o in graph.operators):
        return
    print(f"freezing the variables of {tflite.name}")

    # the values of the variables, by their names, as the subgraph that is called once assigns them
    init = model.subgraphs[[o for o in graph.operators if name_of(o) == ops.CALL_ONCE][0].builtinOptions.initSubgraphIndex]
    init_names = {o.outputs[0]: o.builtinOptions.sharedName for o in init.operators if name_of(o) == ops.VAR_HANDLE}
    values = {init_names[o.inputs[0]]: init.tensors[o.inputs[1]] for o in init.operators if name_of(o) == ops.ASSIGN_VARIABLE}
    handles = {o.outputs[0]: o.builtinOptions.sharedName for o in graph.operators if name_of(o) == ops.VAR_HANDLE}

    def code(builtin):
        for i, c in enumerate(model.operatorCodes):
            if not c.customCode and max(c.builtinCode, c.deprecatedBuiltinCode) == builtin:
                return i
        c = schema.OperatorCodeT()
        c.builtinCode = builtin
        c.deprecatedBuiltinCode = min(builtin, 127)
        c.version = 1
        model.operatorCodes.append(c)
        return len(model.operatorCodes) - 1

    def constant(array, name):
        buffer = schema.BufferT()
        buffer.data = np.frombuffer(np.ascontiguousarray(array).tobytes(), dtype=np.uint8)
        model.buffers.append(buffer)
        return tensor(list(array.shape), schema.TensorType.INT32 if array.dtype == np.int32 else schema.TensorType.FLOAT32,
                      name, len(model.buffers) - 1)

    def tensor(shape, type_, name, buffer=0):
        t = schema.TensorT()
        t.shape = np.asarray(shape, dtype=np.int32)
        t.type = type_
        t.name = name
        t.buffer = buffer
        graph.tensors.append(t)
        return len(graph.tensors) - 1

    def operator(builtin, inputs, outputs, options_type=0, options=None):
        o = schema.OperatorT()
        o.opcodeIndex = code(builtin)
        o.inputs = np.asarray(inputs, dtype=np.int32)
        o.outputs = np.asarray(outputs, dtype=np.int32)
        o.builtinOptionsType = options_type
        o.builtinOptions = options
        return o

    def binary(builtin, options_class, options_type, a, b, out):
        options = options_class()
        options.fusedActivationFunction = schema.ActivationFunctionType.NONE
        return operator(builtin, [a, b], [out], options_type, options)

    operators = []
    noise = 0
    for o in graph.operators:
        n = name_of(o)
        if n in (ops.CALL_ONCE, ops.VAR_HANDLE, ops.ASSIGN_VARIABLE):
            continue
        if n == ops.READ_VARIABLE:
            # the value of the variable, a constant
            value = values[handles[o.inputs[0]]]
            out = graph.tensors[o.outputs[0]]
            buffer = schema.BufferT()
            buffer.data = model.buffers[value.buffer].data
            model.buffers.append(buffer)
            out.buffer = len(model.buffers) - 1
            out.shape = value.shape
            continue
        if n == ops.RANDOM_STANDARD_NORMAL:
            # the noise is an input of the model
            out = graph.tensors[o.outputs[0]]
            out.name = f"noise_{noise}".encode()
            noise += 1
            graph.inputs = np.append(graph.inputs, o.outputs[0]).astype(np.int32)
            continue
        if n == "FusedBatchNormV3":
            options = flexbuffers.GetRoot(bytes(bytearray(o.customOptions))).AsVector
            epsilon, training = options[3].AsFloat, options[5].AsBool
            if not training:
                raise ValueError(f"{tflite.name}: FusedBatchNormV3 that is not training")
            x, scale, offset = o.inputs[:3]
            y = o.outputs[0]
            shape = list(graph.tensors[x].shape)
            channels = [1, 1, 1, shape[3]]
            base = graph.tensors[y].name
            axes = constant(np.asarray([1, 2], dtype=np.int32), base + b"/axes")
            mean = tensor(channels, schema.TensorType.FLOAT32, base + b"/mean")
            squared = tensor(shape, schema.TensorType.FLOAT32, base + b"/squared")
            variance = tensor(channels, schema.TensorType.FLOAT32, base + b"/variance")
            eps = constant(np.asarray([epsilon], dtype=np.float32), base + b"/epsilon")
            shifted = tensor(channels, schema.TensorType.FLOAT32, base + b"/shifted")
            inverse = tensor(channels, schema.TensorType.FLOAT32, base + b"/inverse")
            factor = tensor(channels, schema.TensorType.FLOAT32, base + b"/factor")
            centered = tensor(shape, schema.TensorType.FLOAT32, base + b"/centered")
            scaled = tensor(shape, schema.TensorType.FLOAT32, base + b"/scaled")
            reducer = schema.ReducerOptionsT()
            reducer.keepDims = True
            # (x - mean) / sqrt(variance + epsilon) * scale + offset, by the mean and variance over the pixels
            operators += [
                operator(ops.MEAN, [x, axes], [mean], schema.BuiltinOptions.ReducerOptions, reducer),
                operator(ops.SQUARED_DIFFERENCE, [x, mean], [squared], schema.BuiltinOptions.SquaredDifferenceOptions,
                         schema.SquaredDifferenceOptionsT()),
                operator(ops.MEAN, [squared, axes], [variance], schema.BuiltinOptions.ReducerOptions, reducer),
                binary(ops.ADD, schema.AddOptionsT, schema.BuiltinOptions.AddOptions, variance, eps, shifted),
                operator(ops.RSQRT, [shifted], [inverse]),
                binary(ops.MUL, schema.MulOptionsT, schema.BuiltinOptions.MulOptions, inverse, scale, factor),
                binary(ops.SUB, schema.SubOptionsT, schema.BuiltinOptions.SubOptions, x, mean, centered),
                binary(ops.MUL, schema.MulOptionsT, schema.BuiltinOptions.MulOptions, centered, factor, scaled),
                binary(ops.ADD, schema.AddOptionsT, schema.BuiltinOptions.AddOptions, scaled, offset, y),
            ]
            continue
        operators.append(o)
    graph.operators = operators
    model.subgraphs = [graph]
    flatbuffer_utils.write_model(model, str(tflite))


def fix_resize_sizes(tflite: Path, onnx_path: Path):
    """
    Gives the Resize nodes that tf2onnx made of RESIZE_NEAREST_NEIGHBOR and RESIZE_BILINEAR operators whose scales make
    outputs of the wrong size the sizes of their outputs instead: a scale like 5 / 3 in 32 bits makes an output of 4
    pixels, which an ADD of 5 pixels then does not take, as in EfficientDet-Lite0. The sizes are those of the outputs of
    the TFLite operators with inputs of the same size. Resizes whose scales make outputs of a size of those are kept.
    """
    import numpy as np
    import onnx
    from onnx import numpy_helper, shape_inference
    from tensorflow.lite.python import schema_py_generated as schema
    from tensorflow.lite.tools import flatbuffer_utils

    model = flatbuffer_utils.read_model(str(tflite))
    graph = model.subgraphs[0]
    resizes = {schema.BuiltinOperator.RESIZE_NEAREST_NEIGHBOR, schema.BuiltinOperator.RESIZE_BILINEAR}
    # the sizes of the outputs by the sizes of the inputs, height and width
    sizes = {}
    for o in graph.operators:
        c = model.operatorCodes[o.opcodeIndex]
        if not c.customCode and max(c.builtinCode, c.deprecatedBuiltinCode) in resizes:
            source = tuple(int(v) for v in graph.tensors[o.inputs[0]].shape[1:3])
            target = tuple(int(v) for v in graph.tensors[o.outputs[0]].shape[1:3])
            sizes.setdefault(source, set()).add(target)
    if not sizes:
        return
    m = onnx.load(str(onnx_path))
    initializers = {t.name: numpy_helper.to_array(t) for t in m.graph.initializer}
    fixed = 0
    # the shapes after a resize are known once it has its size, the passes go on until no resize is left
    while True:
        shapes = {v.name: [d.dim_value for d in v.type.tensor_type.shape.dim]
                  for v in list(shape_inference.infer_shapes(m).graph.value_info) + list(m.graph.input)}
        fixed_in_pass = 0
        for node in m.graph.node:
            if node.op_type != "Resize" or len(node.input) < 3 or not node.input[2]:
                continue
            shape = shapes.get(node.input[0])
            # NCHW after the transposes of tf2onnx
            if not shape or len(shape) != 4 or tuple(shape[2:]) not in sizes or node.input[2] not in initializers:
                continue
            scales = initializers[node.input[2]].astype(np.float32)
            # as ONNX Runtime computes them, the scales of 32 bits in 64 bits
            made = tuple(int(np.floor(d * np.float64(scales[i + 2]))) for i, d in enumerate(shape[2:]))
            targets = sizes[tuple(shape[2:])]
            if made in targets:
                continue
            if len(targets) != 1:
                raise ValueError(f"{tflite.name}: a resize of {tuple(shape[2:])} to {made}, not to one of {targets}")
            target = next(iter(targets))
            name = node.name + "/sizes"
            m.graph.initializer.append(numpy_helper.from_array(np.asarray([shape[0], shape[1], *target], dtype=np.int64), name))
            inputs = list(node.input[:2]) + ["", name]
            del node.input[:]
            node.input.extend(inputs)
            fixed_in_pass += 1
        fixed += fixed_in_pass
        if fixed_in_pass == 0:
            break
    if fixed:
        print(f"giving {fixed} resizes of {tflite.name} their sizes")
        # the scales that are no longer used
        used = {i for node in m.graph.node for i in node.input}
        unused = [t for t in m.graph.initializer if t.name not in used]
        for t in unused:
            m.graph.initializer.remove(t)
        onnx.save(m, str(onnx_path))


def write_detector_metadata(model: bytes, path: Path):
    """
    Writes the anchors and the decoding options of an object detector, from the DETECTOR_METADATA in the metadata of the
    model, a flatbuffer of MediaPipe's object_detector_metadata_schema.fbs with the identifier V001: the options as
    `name value` lines, then a line `anchors`, then for each anchor `x_center y_center width height`
    """
    from flatbuffers import number_types
    from flatbuffers.table import Table

    def field(table, index):
        return table.Offset(4 + 2 * index)

    def subtable(table, index):
        o = field(table, index)
        return Table(table.Bytes, table.Indirect(o + table.Pos)) if o else None

    def number(table, index, flags, default=0):
        o = field(table, index)
        return table.Get(flags, o + table.Pos) if o else default

    buffer = bytearray(model)
    start = buffer.find(b"V001")
    while start >= 4:
        try:
            root = Table(buffer, (start - 4) + number_types.UOffsetTFlags.py_type(
                int.from_bytes(buffer[start - 4:start], "little")))
            anchors_schema = subtable(subtable(root, 1), 0)
            decoding = subtable(root, 2)
            o = field(anchors_schema, 0)
            count = anchors_schema.VectorLen(o)
            vector = anchors_schema.Vector(o)
            anchors = []
            for j in range(count):
                anchor = Table(buffer, anchors_schema.Indirect(vector + 4 * j))
                anchors.append([number(anchor, k, number_types.Float32Flags) for k in range(4)])
            names = ["num_classes", "num_boxes", "num_coords", "keypoint_coord_offset", "num_keypoints",
                     "num_values_per_keypoint", "x_scale", "y_scale", "w_scale", "h_scale",
                     "apply_exponential_on_box_size", "sigmoid_score"]
            flags = [number_types.Int32Flags] * 6 + [number_types.Float32Flags] * 4 + [number_types.BoolFlags] * 2
            options = {n: number(decoding, k, f) for k, (n, f) in enumerate(zip(names, flags))}
            if count == options["num_boxes"] > 0:
                with open(path, "w") as out:
                    for n, v in options.items():
                        out.write(f"{n} {int(v) if isinstance(v, bool) else v}\n")
                    out.write("anchors\n")
                    for a in anchors:
                        out.write(" ".join(f"{v:.8g}" for v in a) + "\n")
                print(f"writing {count} anchors of {path.name}")
                return
        except (IndexError, ValueError, TypeError):
            pass
        start = buffer.find(b"V001", start + 1)
    raise ValueError(f"no detector metadata for {path.name}")


def replace_argmax_pooling(tflite: Path, onnx_path: Path):
    """
    Replaces MediaPipe's custom MaxPoolingWithArgmax2D and MaxUnpooling2D operators of the hair segmenter, which tf2onnx
    leaves as nodes that ONNX Runtime does not run, by ONNX MaxPool and MaxUnpool nodes in NCHW between transposes. The
    indices of MaxPool are those of ONNX, which MaxUnpool takes, not those of TensorFlow. The pools are of 2 × 2 pixels
    with strides of 2, as their custom options say.
    """
    import numpy as np
    import onnx
    from onnx import helper
    from tensorflow.lite.tools import flatbuffer_utils

    names = {"MaxPoolingWithArgmax2D", "MaxUnpooling2D"}
    model = flatbuffer_utils.read_model(str(tflite))
    graph = model.subgraphs[0]
    customs = [o for o in graph.operators if (model.operatorCodes[o.opcodeIndex].customCode or b"").decode() in names]
    if not customs:
        return
    for o in customs:
        # padding, stride width, stride height, filter width, filter height
        options = np.frombuffer(bytes(bytearray(o.customOptions)), dtype=np.int32)[1:5].tolist()
        if options != [2, 2, 2, 2]:
            raise ValueError(f"{tflite.name}: a pool of strides and filters {options}")
    print(f"replacing the pools with indices of {tflite.name}")
    m = onnx.load(str(onnx_path))
    nodes = []
    for node in m.graph.node:
        if node.op_type == "TFL_MaxPoolingWithArgmax2D":
            x, (y, indices) = node.input[0], node.output
            nodes += [
                helper.make_node("Transpose", [x], [y + "/nchw_in"], perm=[0, 3, 1, 2]),
                helper.make_node("MaxPool", [y + "/nchw_in"], [y + "/nchw", indices], kernel_shape=[2, 2], strides=[2, 2]),
                helper.make_node("Transpose", [y + "/nchw"], [y], perm=[0, 2, 3, 1]),
            ]
        elif node.op_type == "TFL_MaxUnpooling2D":
            (x, indices), y = node.input, node.output[0]
            nodes += [
                helper.make_node("Transpose", [x], [y + "/nchw_in"], perm=[0, 3, 1, 2]),
                helper.make_node("MaxUnpool", [y + "/nchw_in", indices], [y + "/nchw"], kernel_shape=[2, 2], strides=[2, 2]),
                helper.make_node("Transpose", [y + "/nchw"], [y], perm=[0, 2, 3, 1]),
            ]
        else:
            nodes.append(node)
    del m.graph.node[:]
    m.graph.node.extend(nodes)
    onnx.save(m, str(onnx_path))


def convert(tflite: Path, onnx: Path):
    densify(tflite)
    replace_transpose_convolutions(tflite)
    freeze_variables_and_noise(tflite)
    print(f"converting {tflite.name}")
    subprocess.run([sys.executable, "-m", "tf2onnx.convert", "--tflite", str(tflite), "--output", str(onnx), "--opset", "17"],
                   check=True, capture_output=True)
    fix_resize_sizes(tflite, onnx)
    replace_argmax_pooling(tflite, onnx)


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "models"
    tflite_dir = out / "tflite"
    tflite_dir.mkdir(parents=True, exist_ok=True)

    for name, url in TFLITE.items():
        model = fetch(url)
        (tflite_dir / f"{name}.tflite").write_bytes(model)
        # the labels of classifiers and detectors, in a zip file at the end of the model
        if zipfile.is_zipfile(io.BytesIO(model)):
            with zipfile.ZipFile(io.BytesIO(model)) as files:
                labels = [n for n in files.namelist() if n.startswith("labels")]
                if labels:
                    (out / f"{name}.labels.txt").write_bytes(files.read(labels[0]))
        if b"DETECTOR_METADATA" in model:
            write_detector_metadata(model, out / f"{name}.detector.txt")
    for url, names in TASKS.items():
        with zipfile.ZipFile(io.BytesIO(fetch(url))) as task:
            for name, saved in names.items():
                # a model in a task bundle in the task bundle, like those of the gesture recognizer
                bundle, _, inner = name.rpartition("/")
                if bundle:
                    with zipfile.ZipFile(io.BytesIO(task.read(bundle))) as inner_task:
                        (tflite_dir / f"{saved}.tflite").write_bytes(inner_task.read(f"{inner}.tflite"))
                else:
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
