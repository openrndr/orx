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
}
TASKS = {
    f"{MODELS}/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task": ["hand_detector", "hand_landmarks_detector"],
}
IMAGES = ["portrait.jpg", "thumb_up.jpg", "pointing_up.jpg", "right_hands.jpg"]


def fetch(url: str) -> bytes:
    print(f"downloading {url}")
    with urllib.request.urlopen(url) as r:
        return r.read()


def convert(tflite: Path, onnx: Path):
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
            for name in names:
                (tflite_dir / f"{name}.tflite").write_bytes(task.read(f"{name}.tflite"))
    for tflite in sorted(tflite_dir.glob("*.tflite")):
        convert(tflite, out / f"{tflite.stem}.onnx")

    images = out / "images"
    images.mkdir(exist_ok=True)
    for name in IMAGES:
        (images / name).write_bytes(fetch(f"{ASSETS}/{name}"))
    print(f"models in {out}")


if __name__ == "__main__":
    main()
