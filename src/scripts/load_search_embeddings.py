import json, struct
import numpy as np
from huggingface_hub import hf_hub_download
from safetensors.numpy import load_file

repo = "minishlab/potion-base-8M"
tok_path = hf_hub_download(repo, "tokenizer.json")
weights_path = hf_hub_download(repo, "model.safetensors")

with open(tok_path, "r", encoding="utf-8") as f:
    vocab = json.load(f)["model"]["vocab"]  # token -> id

tensors = load_file(weights_path)
emb = tensors["embeddings"].astype(np.float32)  # [vocab_size, dim]
id_to_tok = {idx: tok for tok, idx in vocab.items()}

vocab_size, dim = emb.shape
with open("erddap_embeddings.bin", "wb") as out:
    out.write(struct.pack(">III", 0x45524456, vocab_size, dim))
    for i in range(vocab_size):
        word_bytes = id_to_tok[i].encode("utf-8")
        row = emb[i]
        scale = float(np.max(np.abs(row)) / 127.0) or 1e-8
        q_row = np.clip(np.round(row / scale), -127, 127).astype(np.int8)
        out.write(struct.pack(">H", len(word_bytes)))
        out.write(word_bytes)
        out.write(struct.pack(">f", scale))
        out.write(q_row.tobytes())
print(f"Wrote erddap_embeddings.bin ({vocab_size} tokens, dim={dim})")