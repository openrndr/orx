package org.openrndr.extra.onnx.models.vae

import org.openrndr.extra.python.createVirtualEnv
import org.openrndr.extra.python.systemPython
import java.io.File
import java.util.Locale

private const val vaeTrainerScript = """
#!/usr/bin/env python3
""${'"'}
Train a variational autoencoder (VAE) on row vectors from a CSV file and export
two ONNX models:

  * encoder.onnx : raw high-dimensional data  -> low-dimensional embedding
  * decoder.onnx : low-dimensional embedding  -> raw high-dimensional data

Normalization is baked into both models: the encoder normalizes its input first,
and the decoder de-normalizes its output, so both ONNX models work directly in the
original data units.

Normalization modes (--normalization):
  minmax   (default) scale each column to [0, 1] using the training data's min/max.
                     The decoder ends in a sigmoid, so its output is guaranteed to
                     stay inside the range seen in the data for ANY latent input.
  standard           z-score each column (mean 0, std 1). The decoder output is
                     unbounded, so far-out latent values can produce huge outputs.

Usage:
    python train_vae.py data.csv --latent-dim 2
    python train_vae.py data.csv --latent-dim 8 --epochs 300 --hidden-dims 256 128
""${'"'}

import argparse
import os

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import DataLoader, TensorDataset, random_split


# --------------------------------------------------------------------------- #
# Data loading and diagnostics
# --------------------------------------------------------------------------- #
def load_csv(path: str, delimiter: str = ",") -> np.ndarray:
    ""${'"'}Load a CSV of row vectors. A non-numeric first row is treated as a header.""${'"'}
    with open(path, newline="") as f:
        first_line = f.readline()

    skiprows = 0
    try:
        [float(v) for v in first_line.strip().split(delimiter)]
    except ValueError:
        skiprows = 1
        print("Detected a header row in the CSV; skipping it.")

    data = np.loadtxt(
        path, delimiter=delimiter, skiprows=skiprows, dtype=np.float32, ndmin=2
    )
    if not np.isfinite(data).all():
        raise ValueError("CSV contains NaN or infinite values; please clean the data.")
    return data


def report_data(data: np.ndarray) -> None:
    ""${'"'}Print a few statistics that reveal common data problems.""${'"'}
    print(f"Data value range: [{data.min():.4g}, {data.max():.4g}]")

    mean, std = data.mean(axis=0), data.std(axis=0)
    flat = np.where(std < 1e-3)[0]
    if len(flat):
        shown = flat[:10].tolist()
        more = " ..." if len(flat) > 10 else ""
        print(f"Warning: {len(flat)} column(s) are near-constant (std < 1e-3): {shown}{more}")

    z = np.abs(data - mean) / np.maximum(std, 1e-6)
    row, col = np.unravel_index(z.argmax(), z.shape)
    print(f"Largest |z-score|: {z[row, col]:.1f} (data row {row}, column {col}, 0-based)")
    if z[row, col] > 20:
        print("Warning: the data has extreme outliers relative to its spread. Z-score "
              "normalization amplifies them; --normalization minmax is safer here.")


# --------------------------------------------------------------------------- #
# Model
# --------------------------------------------------------------------------- #
class Normalizer(nn.Module):
    ""${'"'}Per-feature affine normalization: (x - offset) / scale.
    The statistics are stored as (non-trainable) buffers, so they are saved inside
    the model and exported to ONNX as constants.""${'"'}

    def __init__(self, offset: torch.Tensor, scale: torch.Tensor):
        super().__init__()
        self.register_buffer("offset", offset)
        self.register_buffer("scale", scale)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return (x - self.offset) / self.scale

    def inverse(self, x: torch.Tensor) -> torch.Tensor:
        return x * self.scale + self.offset


def make_mlp(in_dim: int, hidden_dims, out_dim: int) -> nn.Sequential:
    layers, d = [], in_dim
    for h in hidden_dims:
        layers += [nn.Linear(d, h), nn.ReLU()]
        d = h
    layers.append(nn.Linear(d, out_dim))
    return nn.Sequential(*layers)


class VAE(nn.Module):
    def __init__(self, input_dim, latent_dim, hidden_dims, offset, scale, bounded_output):
        super().__init__()
        self.bounded_output = bounded_output
        self.normalizer = Normalizer(offset, scale)
        self.encoder_net = make_mlp(input_dim, hidden_dims, 2 * latent_dim)
        self.decoder_net = make_mlp(latent_dim, list(reversed(hidden_dims)), input_dim)

    # --- inference-time API (raw data units) --------------------------------
    def encode(self, x_raw):
        ""${'"'}raw data -> (mu, logvar) of the approximate posterior.""${'"'}
        h = self.encoder_net(self.normalizer(x_raw))
        mu, logvar = h.chunk(2, dim=-1)
        return mu, logvar.clamp(-10.0, 10.0)  # keeps exp() numerically safe

    def decode_normalized(self, z):
        ""${'"'}latent -> reconstruction in normalized space.""${'"'}
        out = self.decoder_net(z)
        return torch.sigmoid(out) if self.bounded_output else out

    def decode(self, z):
        ""${'"'}latent -> reconstruction in raw data units.""${'"'}
        return self.normalizer.inverse(self.decode_normalized(z))

    # --- training-time API (normalized space) -------------------------------
    def forward(self, x_raw):
        mu, logvar = self.encode(x_raw)
        std = torch.exp(0.5 * logvar)
        z = mu + std * torch.randn_like(std)  # reparameterization trick
        return self.decode_normalized(z), mu, logvar


def vae_loss(model: VAE, x_raw: torch.Tensor, beta: float):
    ""${'"'}Negative ELBO (per sample). Reconstruction error is measured in normalized
    space so every feature contributes on a comparable scale.""${'"'}
    recon_norm, mu, logvar = model(x_raw)
    target = model.normalizer(x_raw)
    recon = F.mse_loss(recon_norm, target, reduction="none").sum(dim=-1).mean()
    kl = (-0.5 * (1 + logvar - mu.pow(2) - logvar.exp()).sum(dim=-1)).mean()
    return recon + beta * kl, recon, kl


@torch.no_grad()
def report_model(model: VAE, x: torch.Tensor) -> None:
    ""${'"'}Deterministic (posterior mean) reconstruction quality on the full dataset.""${'"'}
    mu, logvar = model.encode(x)
    rec = model.decode(mu)
    err = rec - x
    rmse = err.pow(2).mean().sqrt().item()
    print(f"Full-dataset reconstruction: RMSE {rmse:.4f}, max abs error {err.abs().max().item():.4f}")
    print(f"  data range [{x.min().item():.4g}, {x.max().item():.4g}]  "
          f"reconstruction range [{rec.min().item():.4g}, {rec.max().item():.4g}]")
    kl_dim = 0.5 * (mu.pow(2) + logvar.exp() - 1 - logvar).mean(dim=0)
    print(f"  KL per latent dim: {np.round(kl_dim.numpy(), 3).tolist()}")
    if bool((kl_dim < 0.01).all()):
        print("  Warning: all latent dims have ~0 KL (posterior collapse): the embedding "
              "carries no information. Try a smaller --beta.")


# --------------------------------------------------------------------------- #
# ONNX export wrappers
# --------------------------------------------------------------------------- #
class EncoderForExport(nn.Module):
    ""${'"'}raw data -> embedding (posterior mean, deterministic).""${'"'}

    def __init__(self, vae: VAE):
        super().__init__()
        self.vae = vae

    def forward(self, x):
        mu, _ = self.vae.encode(x)
        return mu


class DecoderForExport(nn.Module):
    ""${'"'}embedding -> raw-scale data.""${'"'}

    def __init__(self, vae: VAE):
        super().__init__()
        self.vae = vae

    def forward(self, z):
        return self.vae.decode(z)


def export_onnx(module, dummy, path, input_name, output_name):
    # New torch.export-based exporter (default since PyTorch 2.9; needs `onnxscript`).
    # The batch dimension of the single input is declared dynamic.
    batch = torch.export.Dim("batch")
    torch.onnx.export(
        module,
        (dummy,),
        path,
        input_names=[input_name],
        output_names=[output_name],
        dynamic_shapes=({0: batch},),
        dynamo=True,
    )
    print(f"Exported {path}")


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #
def parse_args():
    p = argparse.ArgumentParser(description="Train a VAE on CSV data and export ONNX encoder/decoder.")
    p.add_argument("csv", help="Input CSV file; each row is one vector.")
    p.add_argument("--latent-dim", type=int, required=True, help="Dimensionality of the embedding.")
    p.add_argument("--output-dir", default=".", help="Where to write encoder.onnx / decoder.onnx.")
    p.add_argument("--normalization", choices=["minmax", "standard"], default="minmax",
                   help="minmax: [0,1] scaling + bounded decoder output (default). "
                        "standard: z-score, unbounded decoder output.")
    p.add_argument("--epochs", type=int, default=200)
    p.add_argument("--batch-size", type=int, default=64)
    p.add_argument("--lr", type=float, default=1e-3)
    p.add_argument("--beta", type=float, default=1.0, help="KL weight (beta-VAE).")
    p.add_argument("--grad-clip", type=float, default=5.0, help="Max gradient norm (0 disables).")
    p.add_argument("--hidden-dims", type=int, nargs="+", default=[128, 64],
                   help="Hidden layer sizes of the encoder (mirrored in the decoder).")
    p.add_argument("--val-split", type=float, default=0.1, help="Fraction held out for validation.")
    p.add_argument("--delimiter", default=",")
    p.add_argument("--seed", type=int, default=0)
    return p.parse_args()


def main():
    args = parse_args()
    torch.manual_seed(args.seed)
    np.random.seed(args.seed)

    # ---- data --------------------------------------------------------------
    data = load_csv(args.csv, args.delimiter)
    n_samples, input_dim = data.shape
    print(f"Loaded {n_samples} samples with {input_dim} features from {args.csv}")
    report_data(data)

    if args.latent_dim < 1:
        raise ValueError("--latent-dim must be >= 1")
    if args.latent_dim >= input_dim:
        print(f"Warning: latent dim ({args.latent_dim}) >= input dim ({input_dim}).")

    x = torch.from_numpy(data)
    if args.normalization == "minmax":
        offset = x.min(dim=0).values
        scale = (x.max(dim=0).values - offset).clamp_min(1e-6)  # constant columns -> tiny range
    else:
        offset = x.mean(dim=0)
        scale = x.std(dim=0, unbiased=False).clamp_min(1e-6)   # constant columns -> tiny scale

    dataset = TensorDataset(x)
    n_val = int(n_samples * args.val_split) if n_samples >= 20 else 0
    n_train = n_samples - n_val
    gen = torch.Generator().manual_seed(args.seed)
    train_set, val_set = random_split(dataset, [n_train, n_val], generator=gen)
    train_loader = DataLoader(train_set, batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(val_set, batch_size=args.batch_size) if n_val else None

    # ---- model -------------------------------------------------------------
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = VAE(input_dim, args.latent_dim, args.hidden_dims, offset, scale,
                bounded_output=(args.normalization == "minmax")).to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr)
    print(f"Training on {device} ({n_train} train / {n_val} val samples, "
          f"{args.normalization} normalization)")

    # ---- training ----------------------------------------------------------
    best_loss, best_state = float("inf"), None
    for epoch in range(1, args.epochs + 1):
        model.train()
        tot, cnt = 0.0, 0
        for (xb,) in train_loader:
            xb = xb.to(device)
            loss, _, _ = vae_loss(model, xb, args.beta)
            optimizer.zero_grad()
            loss.backward()
            if args.grad_clip > 0:
                nn.utils.clip_grad_norm_(model.parameters(), args.grad_clip)
            optimizer.step()
            tot += loss.item() * xb.size(0)
            cnt += xb.size(0)
        train_loss = tot / cnt

        val_loss = None
        if val_loader is not None:
            model.eval()
            tot, cnt = 0.0, 0
            with torch.no_grad():
                for (xb,) in val_loader:
                    xb = xb.to(device)
                    loss, _, _ = vae_loss(model, xb, args.beta)
                    tot += loss.item() * xb.size(0)
                    cnt += xb.size(0)
            val_loss = tot / cnt

        monitored = val_loss if val_loss is not None else train_loss
        if monitored < best_loss:
            best_loss = monitored
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}

        if epoch == 1 or epoch % max(1, args.epochs // 20) == 0 or epoch == args.epochs:
            msg = f"epoch {epoch:4d}/{args.epochs}  train loss {train_loss:.4f}"
            if val_loss is not None:
                msg += f"  val loss {val_loss:.4f}"
            print(msg)

    model.load_state_dict(best_state)
    model = model.cpu().eval()
    report_model(model, x)

    # ---- export ------------------------------------------------------------
    os.makedirs(args.output_dir, exist_ok=True)
    enc_path = os.path.join(args.output_dir, "encoder.onnx")
    dec_path = os.path.join(args.output_dir, "decoder.onnx")

    encoder = EncoderForExport(model).eval()
    decoder = DecoderForExport(model).eval()

    # Batch of 2 (not 1) so the exporter doesn't specialize the batch dimension.
    export_onnx(encoder, torch.zeros(2, input_dim), enc_path, "input", "embedding")
    export_onnx(decoder, torch.zeros(2, args.latent_dim), dec_path, "embedding", "reconstruction")

    # ---- verify ONNX against PyTorch (optional) ----------------------------
    try:
        import onnxruntime as ort
    except ImportError:
        print("onnxruntime not installed; skipping ONNX verification.")
        return

    sample = x[: min(16, n_samples)]
    with torch.no_grad():
        emb_torch = encoder(sample).numpy()
        rec_torch = decoder(torch.from_numpy(emb_torch)).numpy()

    enc_sess = ort.InferenceSession(enc_path)
    dec_sess = ort.InferenceSession(dec_path)
    emb_onnx = enc_sess.run(None, {"input": sample.numpy()})[0]
    rec_onnx = dec_sess.run(None, {"embedding": emb_onnx})[0]

    np.testing.assert_allclose(emb_onnx, emb_torch, rtol=1e-4, atol=1e-4)
    np.testing.assert_allclose(rec_onnx, rec_torch, rtol=1e-4, atol=1e-4)
    rmse = float(np.sqrt(np.mean((rec_onnx - sample.numpy()) ** 2)))
    print(f"ONNX verification passed. Sample reconstruction RMSE (raw units): {rmse:.4f}")

    # Decoder behaviour on arbitrary latent inputs (what an application would feed it).
    z_rand = np.random.randn(1000, args.latent_dim).astype(np.float32)
    out = dec_sess.run(None, {"embedding": z_rand})[0]
    print(f"Decoder output range for 1000 random N(0,1) latents: "
          f"[{out.min():.4g}, {out.max():.4g}]  (data range [{data.min():.4g}, {data.max():.4g}])")


if __name__ == "__main__":
    main()    
"""

class VAETrainer {

    fun train(outputOnnx: File, data: List<DoubleArray>, latentDimensions: Int = 2, epochs: Int = 200, beta: Double = 1.0) {
        val python = systemPython()
        val venv = python.createVirtualEnv("orx-onnx-models")
        venv.install("torch")
        venv.install("onnx")
        venv.install("onnxscript")
        venv.install("onnxruntime")


        val dataStrings = data.map { array ->
            array.joinToString(",") { value ->
                String.format(Locale.ENGLISH, "%.16f", value)
            }
        }.joinToString("\n")

        File("vae-data.csv").writeText(dataStrings)
        File("train-vae.py").writeText(vaeTrainerScript)

        venv.interpreter.invokePython(listOf("train-vae.py", "vae-data.csv",
            "--latent-dim", latentDimensions.toString(),
            "--beta", String.format(Locale.ENGLISH, "%.16f", beta),

        ) )


    }

}